package com.opspilot.ticket.dto;

import com.opspilot.ticket.domain.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating a ticket's status.
 *
 * <p>Only tenant admins may submit this request. No transition validation is enforced at the
 * service layer; any {@link TicketStatus} value is accepted.</p>
 */
public record UpdateTicketStatusRequest(@NotNull TicketStatus status) {
}
