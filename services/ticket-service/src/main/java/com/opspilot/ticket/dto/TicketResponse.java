package com.opspilot.ticket.dto;

import com.opspilot.ticket.domain.entity.Ticket;
import com.opspilot.ticket.domain.entity.TicketOrigin;
import com.opspilot.ticket.domain.entity.TicketStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only response DTO representing a ticket returned to API callers.
 *
 * <p>Constructed from a {@link Ticket} entity via the static factory {@link #fromEntity(Ticket)}.
 * This DTO is the contract surfaced to both authenticated users (via the public API) and to
 * the calling service after an internal ticket creation.</p>
 */
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
    /**
     * Creates a {@link TicketResponse} from the given {@link Ticket} JPA entity.
     *
     * @param ticket the persisted ticket entity to project
     * @return a fully populated response record
     */
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
