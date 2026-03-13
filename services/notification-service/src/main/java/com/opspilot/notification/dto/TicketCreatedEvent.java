package com.opspilot.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record TicketCreatedEvent(
        String requestId,
        UUID ticketId,
        UUID tenantId,
        UUID createdByUserId,
        String createdByEmail,
        String origin,
        String status,
        Double confidence,
        int sourceCount,
        String questionExcerpt,
        Instant createdAt
) {
}
