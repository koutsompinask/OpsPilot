package com.opspilot.assistant.service.embedding;

public record EmbeddingProfile(
        String id,
        String provider,
        String model,
        int dimensions
) {
}
