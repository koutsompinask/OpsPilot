package com.opspilot.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InternalCreateTicketRequest(
        @NotNull UUID tenantId,
        @NotNull UUID createdByUserId,
        @NotBlank @Email @Size(max = 255) String createdByEmail,
        @NotBlank @Size(max = 2000) String question,
        @Size(max = 8000) String answer,
        Double confidence,
        Integer sourceCount,
        @Size(max = 2000) String notes
) {
}
