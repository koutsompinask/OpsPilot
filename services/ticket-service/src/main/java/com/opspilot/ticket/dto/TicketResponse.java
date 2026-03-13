package com.opspilot.ticket.dto;

import com.opspilot.ticket.domain.entity.Ticket;
import com.opspilot.ticket.domain.entity.TicketOrigin;
import com.opspilot.ticket.domain.entity.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID tenantId,
        UUID createdByUserId,
        String createdByEmail,
        TicketOrigin origin,
        TicketStatus status,
        String question,
        String answer,
        Double confidence,
        int sourceCount,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse fromEntity(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getCreatedByUserId(),
                ticket.getCreatedByEmail(),
                ticket.getOrigin(),
                ticket.getStatus(),
                ticket.getQuestion(),
                ticket.getAnswer(),
                ticket.getConfidence(),
                ticket.getSourceCount(),
                ticket.getNotes(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
