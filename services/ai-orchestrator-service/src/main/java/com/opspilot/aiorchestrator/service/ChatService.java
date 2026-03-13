package com.opspilot.aiorchestrator.service;

import com.opspilot.aiorchestrator.dto.ChatAskResponse;
import com.opspilot.aiorchestrator.dto.InternalCreateTicketRequest;
import com.opspilot.aiorchestrator.dto.ChatSourceResponse;
import com.opspilot.aiorchestrator.exception.BadRequestException;
import com.opspilot.aiorchestrator.repository.DocumentChunkSearchRepository;
import com.opspilot.aiorchestrator.repository.RetrievedChunk;
import com.opspilot.aiorchestrator.security.CurrentUser;
import com.opspilot.aiorchestrator.service.answering.AnswerGenerationResult;
import com.opspilot.aiorchestrator.service.answering.AnswerService;
import com.opspilot.aiorchestrator.service.embedding.EmbeddingService;
import com.opspilot.aiorchestrator.service.integration.TicketClient;
import com.opspilot.aiorchestrator.util.logging.RequestCorrelation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final DocumentChunkSearchRepository searchRepository;
    private final EmbeddingService embeddingService;
    private final AnswerService answerService;
    private final TicketClient ticketClient;
    private final int defaultTopK;
    private final double lowConfidenceThreshold;

    public ChatService(
            DocumentChunkSearchRepository searchRepository,
            EmbeddingService embeddingService,
            AnswerService answerService,
            TicketClient ticketClient,
            @Value("${ai.chat.default-top-k:4}") int defaultTopK,
            @Value("${ai.chat.low-confidence-threshold:0.55}") double lowConfidenceThreshold
    ) {
        this.searchRepository = searchRepository;
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

        List<Double> queryEmbedding = embeddingService.provider().embed(List.of(normalizedQuestion)).getFirst();
        List<RetrievedChunk> chunks = searchRepository.searchTopChunks(user.tenantId(), queryEmbedding, topK);
        AnswerGenerationResult answer = answerService.generate(normalizedQuestion, chunks);

        double confidence = computeConfidence(chunks);
        boolean lowConfidence = confidence < lowConfidenceThreshold;
        boolean ticketCreated = false;

        List<ChatSourceResponse> sources = chunks.stream()
                .map(chunk -> new ChatSourceResponse(chunk.documentName(), "chunk-" + chunk.chunkIndex()))
                .toList();

        if (lowConfidence) {
            ticketCreated = createSupportTicket(user, normalizedQuestion, answer.answer(), confidence, sources.size());
        }

        log.info(
                "ai_chat_response_ready tenantId={} userId={} chunkCount={} confidence={} lowConfidence={} answerProvider={} ticketCreated={}",
                user.tenantId(),
                user.userId(),
                chunks.size(),
                confidence,
                lowConfidence,
                answer.provider(),
                ticketCreated
        );

        return new ChatAskResponse(answer.answer(), confidence, sources, ticketCreated);
    }

    private int normalizeTopK(Integer requestedTopK) {
        int topK = requestedTopK == null ? defaultTopK : requestedTopK;
        if (topK < 1 || topK > 10) {
            throw new BadRequestException("topK must be between 1 and 10");
        }
        return topK;
    }

    private double computeConfidence(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return 0.0;
        }

        int sampleSize = Math.min(3, chunks.size());
        double total = 0.0;
        for (int i = 0; i < sampleSize; i++) {
            double distance = chunks.get(i).distance();
            double boundedDistance = Math.max(0.0, Math.min(2.0, distance));
            total += 1.0 - (boundedDistance / 2.0);
        }
        double avg = total / sampleSize;
        return Math.round(avg * 1000.0) / 1000.0;
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
