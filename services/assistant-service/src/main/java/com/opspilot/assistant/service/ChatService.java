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

/**
 * Orchestrates the full RAG (Retrieval-Augmented Generation) pipeline for a single chat query.
 *
 * <p>Processing order per request:
 * <ol>
 *   <li>Ensure the tenant's documents are indexed under the active embedding profile,
 *       scheduling re-indexing if the profile has changed.</li>
 *   <li>Embed the question and retrieve the top-K most relevant chunks via hybrid
 *       vector + lexical search followed by neural reranking.</li>
 *   <li>Generate a natural-language answer from the ranked evidence chunks using the
 *       configured {@link AnswerService} provider.</li>
 *   <li>Compute a confidence score and, if it falls below the configured threshold,
 *       automatically escalate by creating a support ticket via {@link TicketClient}.</li>
 * </ol>
 * </p>
 */
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

    /**
     * Executes the full RAG pipeline for a user question and returns the answer with evidence.
     *
     * <p>If the active embedding profile has changed since documents were last indexed,
     * re-indexing is triggered automatically before retrieval. When no ready documents
     * exist yet under the new profile (re-indexing still in progress), a placeholder
     * response is returned immediately instead of attempting retrieval against an empty index.</p>
     *
     * @param user           the authenticated caller, used for tenant scoping and ticket creation
     * @param question       the natural-language question; {@code null} is treated as an empty string
     * @param requestedTopK  the number of evidence chunks to retrieve; must be 1–10 or {@code null}
     *                       to fall back to the default configured by {@code ai.chat.default-top-k}
     * @return the complete chat response including answer text, confidence score, sources,
     *         evidence snippets, and a flag indicating whether a support ticket was created
     * @throws com.opspilot.assistant.exception.BadRequestException if {@code requestedTopK} is outside the valid range
     */
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

        // If re-indexing was just scheduled and no documents are ready under the new profile yet,
        // skip retrieval and return a "retry later" response rather than returning empty results.
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
        // Treat the answer as low-confidence if the numeric score is below the threshold OR
        // if the answer generator explicitly signalled that it lacked sufficient evidence.
        // Configured via ai.chat.low-confidence-threshold (default 0.55).
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

        // Average the reranker scores of the top-3 chunks (or fewer if less were retrieved).
        // Using only the top-3 rather than all chunks avoids diluting the score with weak
        // tail evidence that the reranker already ranked low.
        int sampleSize = Math.min(3, chunks.size());
        double total = 0.0;
        for (int i = 0; i < sampleSize; i++) {
            total += chunks.get(i).rerankerScore();
        }

        double avg = total / sampleSize;
        // Clamp to [0.0, 1.0] and round to 3 decimal places for a stable, display-friendly value
        double normalized = Math.max(0.0, Math.min(1.0, avg));
        return Math.round(normalized * 1000.0) / 1000.0;
    }

    private String trimSnippet(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        // 240-char cap keeps evidence snippets readable in the UI without truncating too aggressively;
        // the trailing "..." makes truncation visible to the end user.
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
