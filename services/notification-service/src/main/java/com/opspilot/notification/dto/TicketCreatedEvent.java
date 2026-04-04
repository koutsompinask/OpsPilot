package com.opspilot.notification.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event payload received when a support ticket is created in the ticket-service.
 *
 * <p>Published by the ticket-service to the {@code opspilot.events} exchange with routing key
 * {@code ticket.created}. The {@code confidence} and {@code sourceCount} fields carry
 * AI-assistant metadata from the originating conversation, allowing webhook consumers to
 * surface low-confidence escalation information in external systems.</p>
 *
 * @param requestId       correlation ID propagated from the originating HTTP request
 * @param ticketId        unique identifier of the newly created ticket
 * @param tenantId        tenant that owns the ticket
 * @param createdByUserId user who triggered ticket creation
 * @param createdByEmail  email address of the creating user
 * @param origin          how the ticket was created (e.g. {@code AUTO_ESCALATION}, {@code MANUAL})
 * @param status          initial ticket status (e.g. {@code OPEN})
 * @param confidence      AI confidence score at the time of escalation, if applicable
 * @param sourceCount     number of RAG source chunks cited in the assistant response
 * @param questionExcerpt truncated text of the user question that prompted ticket creation
 * @param createdAt       wall-clock timestamp at which the ticket was persisted
 */
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
