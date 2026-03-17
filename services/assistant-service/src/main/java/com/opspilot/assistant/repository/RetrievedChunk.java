package com.opspilot.assistant.repository;

import java.util.UUID;

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
