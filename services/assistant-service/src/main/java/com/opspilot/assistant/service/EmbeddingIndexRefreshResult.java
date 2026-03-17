package com.opspilot.assistant.service;

public record EmbeddingIndexRefreshResult(
        int scheduledCount,
        long readyDocumentCount
) {
}
