package com.opspilot.assistant.repository;

import java.util.UUID;

/**
 * An immutable result record produced by the retrieval queries in {@link DocumentChunkSearchRepository}.
 *
 * Carries both the raw chunk content and the scores computed at different pipeline stages:
 * <ul>
 *   <li>{@code vectorDistance} — cosine distance from the query embedding (lower = more similar); null for lexical-only results</li>
 *   <li>{@code lexicalScore} — full-text rank score from PostgreSQL ts_rank_cd; null for vector-only results</li>
 *   <li>{@code retrievalScore} — the fused RRF score assigned by {@link com.opspilot.assistant.service.retrieval.ChunkRetrievalService}</li>
 *   <li>{@code rerankerScore} — the final score assigned by the reranker; 0.0 before reranking</li>
 * </ul>
 *
 * Records are immutable; use {@link #withRetrievalScore} and {@link #withRerankerScore} to
 * create updated copies without modifying the original.
 */
public record RetrievedChunk(
        UUID documentId,
        String documentName,
        int chunkIndex,
        String sectionTitle,
        String chunkType,
        String chunkText,
        Double vectorDistance,
        Double lexicalScore,
        double retrievalScore,
        double rerankerScore
) {
    public RetrievedChunk withRetrievalScore(double value) {
        return new RetrievedChunk(
                documentId,
                documentName,
                chunkIndex,
                sectionTitle,
                chunkType,
                chunkText,
                vectorDistance,
                lexicalScore,
                value,
                rerankerScore
        );
    }

    public RetrievedChunk withRerankerScore(double value) {
        return new RetrievedChunk(
                documentId,
                documentName,
                chunkIndex,
                sectionTitle,
                chunkType,
                chunkText,
                vectorDistance,
                lexicalScore,
                retrievalScore,
                value
        );
    }
}
