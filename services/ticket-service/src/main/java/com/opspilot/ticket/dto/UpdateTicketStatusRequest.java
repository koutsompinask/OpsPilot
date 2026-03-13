package com.opspilot.ticket.dto;

import com.opspilot.ticket.domain.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(@NotNull TicketStatus status) {
}
