package com.opspilot.assistant.dto;

/**
 * A single evidence item returned in the {@link ChatAskResponse}, representing one chunk
 * that contributed to the answer.
 *
 * @param document       the source document filename
 * @param chunkId        a string identifier for the chunk ({@code documentId:chunkIndex})
 * @param sectionTitle   the section heading this chunk belongs to, if any
 * @param snippet        a short excerpt of the chunk text
 * @param relevanceScore the reranker score for this chunk
 */
public record ChatEvidenceResponse(
        String document,
        String chunkId,
        String sectionTitle,
        String snippet,
        double relevanceScore
) {
}
