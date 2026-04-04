package com.opspilot.ticket.controller;

import com.opspilot.ticket.dto.CreateTicketRequest;
import com.opspilot.ticket.dto.TicketResponse;
import com.opspilot.ticket.dto.UpdateTicketStatusRequest;
import com.opspilot.ticket.exception.ForbiddenException;
import com.opspilot.ticket.security.CurrentUser;
import com.opspilot.ticket.security.CurrentUserResolver;
import com.opspilot.ticket.service.TicketService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

/**
 * REST controller exposing the public ticket management API under {@code /tickets}.
 *
 * <p>All endpoints require a valid JWT bearer token. Tenant isolation is enforced by scoping
 * every operation to the {@link CurrentUser#tenantId()} extracted from the token — users never
 * see or modify tickets belonging to other tenants. Write operations (create, status update) are
 * further restricted to users with the {@link com.opspilot.ticket.domain.entity.Role#TENANT_ADMIN}
 * role.</p>
 */
@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CurrentUserResolver currentUserResolver;

    public TicketController(TicketService ticketService, CurrentUserResolver currentUserResolver) {
        this.ticketService = ticketService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Returns a page of tickets belonging to the caller's tenant.
     *
     * <p>Defaults to 20 tickets per page, sorted by {@code createdAt} descending. Callers can
     * override via standard Spring {@code ?page=}, {@code ?size=}, and {@code ?sort=} query params.
     *
     * @param jwt      the verified JWT principal injected by Spring Security
     * @param pageable pagination and sort parameters; defaults to page 0, size 20, newest-first
     * @return a page of ticket responses scoped to the caller's tenant
     */
    @GetMapping
    public Page<TicketResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        return ticketService.list(currentUser, pageable);
    }

    /**
     * Creates a new ticket manually on behalf of the authenticated tenant admin.
     *
     * <p>Only tenant admins may create manual tickets. Regular members must rely on the
     * auto-escalation path via the assistant-service instead.</p>
     *
     * @param jwt     the verified JWT principal injected by Spring Security
     * @param request the ticket creation payload, validated before processing
     * @return the newly created ticket, HTTP 201
     * @throws ForbiddenException if the caller does not hold the TENANT_ADMIN role
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTicketRequest request) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        // Only admins can raise tickets manually; members interact via the chat interface
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can create manual tickets");
        }
        return ticketService.createManual(currentUser, request);
    }

    /**
     * Updates the status of an existing ticket.
     *
     * <p>Status updates are restricted to tenant admins because status transitions represent
     * operational decisions (e.g., marking a ticket resolved) that regular users should not
     * be able to perform unilaterally.</p>
     *
     * @param jwt      the verified JWT principal injected by Spring Security
     * @param ticketId the UUID of the ticket to update
     * @param request  the new status, validated before processing
     * @return the updated ticket
     * @throws ForbiddenException if the caller does not hold the TENANT_ADMIN role
     * @throws com.opspilot.ticket.exception.NotFoundException if no ticket with the given ID
     *         exists within the caller's tenant
     */
    @PatchMapping("/{ticketId}/status")
    public TicketResponse updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.fromJwt(jwt);
        // Status changes reflect workflow decisions — only admins are authorised to make them
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only tenant admins can update ticket status");
        }
        return ticketService.updateStatus(currentUser, ticketId, request);
    }
}
