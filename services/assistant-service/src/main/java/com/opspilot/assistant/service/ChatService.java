package com.opspilot.assistant.service;

import com.opspilot.assistant.dto.ChatAskResponse;
import com.opspilot.assistant.dto.ChatEvidenceResponse;
import com.opspilot.assistant.dto.ChatSourceResponse;
import com.opspilot.assistant.dto.InternalCreateTicketRequest;
import com.opspilot.assistant.exception.BadRequestException;
import com.opspilot.assistant.repository.RetrievedChunk;
import com.opspilot.assistant.security.CurrentUser;
import com.opspilot.assistant.service.answering.AnswerGenerationResult;
import com.opspilot.assistant.service.answering.AnswerService;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import com.opspilot.assistant.service.integration.TicketClient;
import com.opspilot.assistant.service.retrieval.ChunkRetrievalService;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChunkRetrievalService chunkRetrievalService;
    private final DocumentEmbeddingMaintenanceService documentEmbeddingMaintenanceService;
    private final EmbeddingService embeddingService;
    private final AnswerService answerService;
    private final TicketClient ticketClient;
    private final int defaultTopK;
    private final double lowConfidenceThreshold;

    public ChatService(
            ChunkRetrievalService chunkRetrievalService,
            DocumentEmbeddingMaintenanceService documentEmbeddingMaintenanceService,
            EmbeddingService embeddingService,
            AnswerService answerService,
            TicketClient ticketClient,
            @Value("${ai.chat.default-top-k:4}") int defaultTopK,
            @Value("${ai.chat.low-confidence-threshold:0.55}") double lowConfidenceThreshold
    ) {
        this.chunkRetrievalService = chunkRetrievalService;
        this.documentEmbeddingMaintenanceService = documentEmbeddingMaintenanceService;
        this.embeddingService = embeddingService;
        this.answerService = answerService;
        this.ticketClient = ticketClient;
        this.defaultTopK = defaultTopK;
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }

    public ChatAskResponse ask(CurrentUser user, String question, Integer requestedTopK) {
        int topK = normalizeTopK(requestedTopK);
        String normalizedQuestion = question == null ? "" : question.trim();

        log.info(
                "ai_chat_request_received tenantId={} userId={} questionLength={} topK={}",
                user.tenantId(),
                user.userId(),
                normalizedQuestion.length(),
                topK
        );

        EmbeddingIndexRefreshResult refreshResult = documentEmbeddingMaintenanceService.ensureCurrentProfile(
                user.tenantId(),
                RequestCorrelation.currentRequestId()
        );
        String activeProfile = embeddingService.profile().id();
        List<RetrievedChunk> chunks = chunkRetrievalService.retrieve(user.tenantId(), normalizedQuestion, topK);

        if (chunks.isEmpty() && refreshResult.scheduledCount() > 0 && refreshResult.readyDocumentCount() == 0) {
            log.info(
                    "ai_chat_reindex_in_progress tenantId={} userId={} scheduledCount={} profile={}",
                    user.tenantId(),
                    user.userId(),
                    refreshResult.scheduledCount(),
                    activeProfile
            );
            return new ChatAskResponse(
                    "Your knowledge base is being re-indexed for the active embedding model. Please retry in a moment.",
                    "The active embedding profile has no ready documents yet, so retrieval was deferred until re-indexing completes.",
                    0.0,
                    List.of(),
                    List.of(),
                    "insufficient-evidence",
                    false
            );
        }

        AnswerGenerationResult answer = answerService.generate(normalizedQuestion, chunks);

        double confidence = computeConfidence(chunks, answer);
        boolean lowConfidence = confidence < lowConfidenceThreshold || "insufficient-evidence".equals(answer.answerMode());
        boolean ticketCreated = false;

        List<ChatSourceResponse> sources = chunks.stream()
                .map(chunk -> new ChatSourceResponse(chunk.documentName(), "chunk-" + chunk.chunkIndex()))
                .toList();

        List<ChatEvidenceResponse> evidence = chunks.stream()
                .map(chunk -> new ChatEvidenceResponse(
                        chunk.documentName(),
                        "chunk-" + chunk.chunkIndex(),
                        chunk.sectionTitle(),
                        trimSnippet(chunk.chunkText()),
                        chunk.rerankerScore()
                ))
                .toList();

        if (lowConfidence) {
            ticketCreated = createSupportTicket(user, normalizedQuestion, answer.answer(), confidence, sources.size());
        }

        log.info(
                "ai_chat_response_ready tenantId={} userId={} chunkCount={} confidence={} lowConfidence={} answerProvider={} answerMode={} ticketCreated={}",
                user.tenantId(),
                user.userId(),
                chunks.size(),
                confidence,
                lowConfidence,
                answer.provider(),
                answer.answerMode(),
                ticketCreated
        );

        return new ChatAskResponse(
                answer.answer(),
                answer.reasoningSummary(),
                confidence,
                sources,
                evidence,
                answer.answerMode(),
                ticketCreated
        );
    }

    private int normalizeTopK(Integer requestedTopK) {
        int topK = requestedTopK == null ? defaultTopK : requestedTopK;
        if (topK < 1 || topK > 10) {
            throw new BadRequestException("topK must be between 1 and 10");
        }
        return topK;
    }

    private double computeConfidence(List<RetrievedChunk> chunks, AnswerGenerationResult answer) {
        if (chunks.isEmpty() || "insufficient-evidence".equals(answer.answerMode())) {
            return 0.0;
        }

        int sampleSize = Math.min(3, chunks.size());
        double total = 0.0;
        for (int i = 0; i < sampleSize; i++) {
            total += chunks.get(i).rerankerScore();
        }

        double avg = total / sampleSize;
        double normalized = Math.max(0.0, Math.min(1.0, avg));
        return Math.round(normalized * 1000.0) / 1000.0;
    }

    private String trimSnippet(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 237).trim() + "...";
    }

    private boolean createSupportTicket(
            CurrentUser user,
            String question,
            String answer,
            double confidence,
            int sourceCount
    ) {
        try {
            ticketClient.createTicket(new InternalCreateTicketRequest(
                    user.tenantId(),
                    user.userId(),
                    user.email(),
                    question,
                    answer,
                    confidence,
                    sourceCount,
                    "Auto-created from low-confidence chat response"
            ));
            return true;
        } catch (Exception ex) {
            log.error(
                    "ai_chat_ticket_create_failed tenantId={} userId={} confidence={} sourceCount={} requestId={} reason={}",
                    user.tenantId(),
                    user.userId(),
                    confidence,
                    sourceCount,
                    RequestCorrelation.currentRequestId(),
                    ex.getMessage(),
                    ex
            );
            return false;
        }
    }
}
