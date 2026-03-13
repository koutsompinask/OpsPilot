package com.opspilot.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 2000) String question,
        @Size(max = 8000) String answer,
        Double confidence,
        Integer sourceCount,
        @Size(max = 2000) String notes
) {
}
