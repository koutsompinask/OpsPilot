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

@RestController
@RequestMapping("/internal/tickets")
public class InternalTicketController {

    private final TicketService ticketService;
    private final String serviceToken;

    public InternalTicketController(TicketService ticketService, @Value("${ticket.service-token}") String serviceToken) {
        this.ticketService = ticketService;
        this.serviceToken = serviceToken;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(
            @RequestHeader(name = "X-Service-Token", required = false) String providedToken,
            @Valid @RequestBody InternalCreateTicketRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!serviceToken.equals(providedToken)) {
            throw new ForbiddenException("Invalid service token");
        }
        return ticketService.createInternal(request, httpRequest.getHeader(RequestCorrelation.HEADER_NAME));
    }
}
