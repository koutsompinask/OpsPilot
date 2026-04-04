package com.opspilot.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request payload for creating a ticket via the internal service-to-service endpoint.
 *
 * <p>Sent by the assistant-service when a chat response's confidence score falls below the
 * escalation threshold. Unlike the public creation request, the caller must supply the tenant
 * and user context explicitly because the internal endpoint does not carry a user JWT — it
 * is authenticated via the shared service token instead.</p>
 */
public record InternalCreateTicketRequest(
        @NotNull UUID tenantId,
        @NotNull UUID createdByUserId,
        @NotBlank @Email @Size(max = 255) String createdByEmail,
        @NotBlank @Size(max = 2000) String question,
        @Size(max = 8000) String answer,
        /** Confidence score from the assistant's response, in the range [0, 1]. */
        Double confidence,
        /** Number of knowledge-base sources cited in the assistant's answer. */
        Integer sourceCount,
        @Size(max = 2000) String notes
) {
}
