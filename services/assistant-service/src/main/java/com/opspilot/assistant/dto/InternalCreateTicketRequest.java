package com.opspilot.assistant.dto;

import java.util.UUID;

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
