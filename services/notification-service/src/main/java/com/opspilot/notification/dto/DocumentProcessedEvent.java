package com.opspilot.notification.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event payload received when a document completes the ingestion pipeline in the assistant-service.
 *
 * <p>Published by the assistant-service to the {@code opspilot.events} exchange with routing key
 * {@code document.processed} after a document has been chunked, embedded, and stored in the
 * vector store. The {@code chunkCount} field indicates how many searchable chunks were produced.</p>
 *
 * @param requestId   correlation ID propagated from the originating ingestion request
 * @param tenantId    tenant that owns the document
 * @param documentId  unique identifier of the processed document
 * @param chunkCount  number of text chunks created and indexed for this document
 * @param processedAt wall-clock timestamp at which ingestion completed
 */
public record DocumentProcessedEvent(
        String requestId,
        UUID tenantId,
        UUID documentId,
        int chunkCount,
        Instant processedAt
) {
}
