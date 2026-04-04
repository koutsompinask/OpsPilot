package com.opspilot.assistant.dto;

import java.util.List;

/**
 * Response body for {@code POST /chat/ask}, carrying the generated answer and all evidence metadata.
 *
 * @param answer           the LLM- or extractive-generated answer text
 * @param reasoningSummary a brief explanation of the evidence used to produce the answer
 * @param confidence       a score in [0, 1] reflecting how strongly the reranker supports the answer; low values trigger ticket creation
 * @param sources          deduplicated source document references for display
 * @param evidence         detailed per-chunk evidence with relevance scores for developer/debug views
 * @param answerMode       the generation strategy used (e.g. {@code "llm-grounded"}, {@code "insufficient-evidence"})
 * @param ticketCreated    {@code true} if a support ticket was automatically created due to low confidence
 */
public record ChatAskResponse(
        String answer,
        String reasoningSummary,
        double confidence,
        List<ChatSourceResponse> sources,
        List<ChatEvidenceResponse> evidence,
        String answerMode,
        boolean ticketCreated
) {
}
