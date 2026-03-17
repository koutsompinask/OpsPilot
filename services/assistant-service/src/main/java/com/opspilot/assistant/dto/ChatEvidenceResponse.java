package com.opspilot.assistant.dto;

public record ChatEvidenceResponse(
        String document,
        String chunkId,
        String sectionTitle,
        String snippet,
        double relevanceScore
) {
}
