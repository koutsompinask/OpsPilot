package com.opspilot.assistant.dto;

import java.util.UUID;

/**
 * Internal request payload sent by the assistant-service to the ticket-service when
 * a chat answer falls below the low-confidence threshold.
 *
 * This DTO is posted to {@code POST /internal/tickets} and is authenticated via
 * the {@code X-Service-Token} header.
 *
 * @param tenantId        the tenant on whose behalf the ticket is created
 * @param createdByUserId the user who asked the question that triggered escalation
 * @param createdByEmail  the user's email, stored in the ticket for agent follow-up
 * @param question        the original question that could not be answered confidently
 * @param answer          the low-confidence answer that was returned to the user
 * @param confidence      the reranker confidence score that triggered escalation
 * @param sourceCount     the number of retrieved source chunks that informed the answer
 * @param notes           any additional context to include in the ticket
 */
public record InternalCreateTicketRequest(
        UUID tenantId,
        UUID createdByUserId,
        String createdByEmail,
        String question,
        String answer,
        Double confidence,
        Integer sourceCount,
        String notes
) {
}
