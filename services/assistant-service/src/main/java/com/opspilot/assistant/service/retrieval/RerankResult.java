package com.opspilot.assistant.service.retrieval;

/**
 * Holds the reranking score for a single passage.
 *
 * @param index the zero-based position of the passage in the original input list
 * @param score relevance score normalised to the range {@code [0.0, 1.0]}; higher is more relevant
 */
public record RerankResult(
        int index,
        double score
) {
}
