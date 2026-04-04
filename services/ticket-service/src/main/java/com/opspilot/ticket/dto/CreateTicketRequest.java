package com.opspilot.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for manually creating a support ticket via the public API.
 *
 * <p>Used by tenant admins to raise tickets outside of the auto-escalation flow. The
 * {@code confidence} and {@code sourceCount} fields are optional but may be supplied when the
 * admin is documenting a specific chat exchange that warranted escalation.</p>
 */
public record CreateTicketRequest(
        @NotBlank @Size(max = 2000) String question,
        @Size(max = 8000) String answer,
        /** Confidence score in the range [0, 1]; {@code null} if not applicable. */
        Double confidence,
        /** Number of knowledge-base sources cited in the assistant's answer; {@code null} if unknown. */
        Integer sourceCount,
        @Size(max = 2000) String notes
) {
}
