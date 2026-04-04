package com.opspilot.assistant.service;

/**
 * Summary result returned after triggering an embedding index refresh.
 *
 * @param scheduledCount    the number of documents queued for re-embedding in this refresh cycle
 * @param readyDocumentCount the total number of documents currently in READY state for the tenant
 */
public record EmbeddingIndexRefreshResult(
        int scheduledCount,
        long readyDocumentCount
) {
}
