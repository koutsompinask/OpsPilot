package com.opspilot.ticket.controller;

import com.opspilot.ticket.dto.CreateTicketRequest;
import com.opspilot.ticket.dto.TicketResponse;
import com.opspilot.ticket.dto.UpdateTicketStatusRequest;
import com.opspilot.ticket.exception.ForbiddenException;
import com.opspilot.ticket.security.CurrentUser;
import com.opspilot.ticket.security.CurrentUserResolver;
import com.opspilot.ticket.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CurrentUserResolver currentUserResolver;

    public TicketController(TicketService ticketService, CurrentUserResolver currentUserResolver) {
        this.ticketService = ticketService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public List<TicketResponse> list(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        return ticketService.list(currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTicketRequest request) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can create manual tickets");
        }
        return ticketService.createManual(currentUser, request);
    }

    @PatchMapping("/{ticketId}/status")
    public TicketResponse updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can update ticket status");
        }
        return ticketService.updateStatus(currentUser, ticketId, request);
    }
}
