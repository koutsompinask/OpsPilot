package com.opspilot.assistant.service.integration;

import com.opspilot.assistant.dto.InternalCreateTicketRequest;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;

/**
 * REST client used by the assistant-service to create support tickets in the ticket-service.
 *
 * Tickets are created automatically when a chat answer has a confidence score below the
 * configured low-confidence threshold ({@code ai.chat.low-confidence-threshold}).
 * Authentication is via the {@code X-Service-Token} header, which must match the shared
 * {@code INTERNAL_SERVICE_TOKEN} configured across services.
 */
@Component
public class TicketClient {

    private static final Logger log = LoggerFactory.getLogger(TicketClient.class);

    private final RestClient restClient;
    private final String serviceToken;

    public TicketClient(
            RestClient.Builder builder,
            @Value("${ticket-service.base-url}") String ticketServiceBaseUrl,
            @Value("${ticket-service.service-token}") String serviceToken
    ) {
        this.restClient = builder.baseUrl(ticketServiceBaseUrl).build();
        this.serviceToken = serviceToken;
    }

    /**
     * Creates a support ticket in the ticket-service for a low-confidence chat answer.
     *
     * @param request the ticket creation payload including tenant, user, question, answer, and confidence metadata
     * @throws IllegalStateException if the ticket-service returns a non-2xx response
     */
    public void createTicket(InternalCreateTicketRequest request) {
        String requestId = RequestCorrelation.currentRequestId();
        log.info(
                "ticket_client_create_requested tenantId={} userId={} sourceCount={} confidence={}",
                request.tenantId(),
                request.createdByUserId(),
                request.sourceCount(),
                request.confidence()
        );
        RequestBodySpec requestSpec = restClient.post()
                .uri("/internal/tickets")
                .header("X-Service-Token", serviceToken)
                .header(RequestCorrelation.HEADER_NAME, requestId);
        requestSpec.body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException("Ticket create failed with status " + res.getStatusCode());
                })
                .toBodilessEntity();
        log.info("ticket_client_create_succeeded tenantId={} userId={} requestId={}", request.tenantId(), request.createdByUserId(), requestId);
    }
}
