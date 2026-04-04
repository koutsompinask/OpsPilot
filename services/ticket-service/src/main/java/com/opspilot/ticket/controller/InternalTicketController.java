package com.opspilot.ticket.controller;

import com.opspilot.ticket.dto.InternalCreateTicketRequest;
import com.opspilot.ticket.dto.TicketResponse;
import com.opspilot.ticket.exception.ForbiddenException;
import com.opspilot.ticket.service.TicketService;
import com.opspilot.ticket.util.logging.RequestCorrelation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal REST controller that allows trusted services to create tickets programmatically.
 *
 * <p>This endpoint is mounted under {@code /internal/tickets} and is intentionally excluded from
 * Spring Security's JWT validation (see {@link com.opspilot.ticket.config.SecurityConfig}). Instead,
 * it uses a shared secret service-token pattern: the caller must supply the token configured in
 * {@code ticket.service-token} via the {@code X-Service-Token} request header. This keeps
 * internal traffic lightweight without requiring each service to obtain a user JWT.</p>
 *
 * <p>The primary caller is the assistant-service, which invokes this endpoint when a chat
 * response's confidence score falls below the auto-escalation threshold.</p>
 */
@RestController
@RequestMapping("/internal/tickets")
public class InternalTicketController {

    private final TicketService ticketService;

    // Pre-shared token loaded from configuration; compared on every request
    private final String serviceToken;

    public InternalTicketController(TicketService ticketService, @Value("${ticket.service-token}") String serviceToken) {
        this.ticketService = ticketService;
        this.serviceToken = serviceToken;
    }

    /**
     * Creates a ticket on behalf of an internal service caller (typically the assistant-service).
     *
     * <p>Authentication is performed via a static service token passed in the
     * {@code X-Service-Token} header. The correlation ID from the originating chat request is
     * forwarded so that the resulting ticket can be traced back to the chat session in logs.</p>
     *
     * @param providedToken the service token supplied by the caller; {@code null} if the header
     *                      was omitted
     * @param request       the ticket payload provided by the calling service, validated before
     *                      processing
     * @param httpRequest   the raw HTTP request used to extract the correlation ID header
     * @return the newly created ticket, HTTP 201
     * @throws ForbiddenException if {@code providedToken} does not match the configured secret
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(
            @RequestHeader(name = "X-Service-Token", required = false) String providedToken,
            @Valid @RequestBody InternalCreateTicketRequest request,
            HttpServletRequest httpRequest
    ) {
        // Service-token check: reject the request immediately if the token is wrong or missing
        if (!serviceToken.equals(providedToken)) {
            throw new ForbiddenException("Invalid service token");
        }
        // Forward the correlation ID so the ticket can be linked to the originating chat request
        return ticketService.createInternal(request, httpRequest.getHeader(RequestCorrelation.HEADER_NAME));
    }
}
