package com.opspilot.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentProcessedEvent(
        String requestId,
        UUID tenantId,
        UUID documentId,
        int chunkCount,
        Instant processedAt
) {
}
