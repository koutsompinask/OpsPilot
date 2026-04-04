package com.opspilot.ticket.service.messaging;

import com.opspilot.ticket.domain.entity.Ticket;
import com.opspilot.ticket.domain.entity.TicketOrigin;
import com.opspilot.ticket.domain.entity.TicketStatus;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code ticket.created} events to RabbitMQ after a ticket is persisted.
 *
 * <p>Events are serialised as JSON by the {@link Jackson2JsonMessageConverter} configured in
 * {@link MessagingConfig} and routed to the notification-service via the
 * {@code opspilot.events} direct exchange using the {@code ticket.created} routing key.
 * Publishing can be disabled via {@code ticket.messaging.enabled=false} for environments
 * where RabbitMQ is not available.</p>
 */
@Component
public class TicketCreatedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TicketCreatedEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messagingProperties;

    public TicketCreatedEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties messagingProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingProperties = messagingProperties;
    }

    /**
     * Builds and dispatches a {@link TicketCreatedEvent} to RabbitMQ.
     *
     * <p>Publication is a best-effort fire-and-forget operation. If the broker is unavailable,
     * the exception will propagate to the caller and the enclosing transaction will be rolled
     * back. The question field is truncated to 240 characters in the event payload to keep
     * message sizes manageable.</p>
     *
     * @param ticket the newly persisted ticket whose data is included in the event
     */
    public void publish(Ticket ticket) {
        // Skip publishing in environments where messaging is disabled (e.g. local dev without RabbitMQ)
        if (!messagingProperties.isEnabled()) {
            return;
        }

        TicketCreatedEvent event = new TicketCreatedEvent(
                ticket.getCreatedRequestId(),
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getCreatedByUserId(),
                ticket.getCreatedByEmail(),
                ticket.getOrigin(),
                ticket.getStatus(),
                ticket.getConfidence(),
                ticket.getSourceCount(),
                toExcerpt(ticket.getQuestion()),
                ticket.getCreatedAt()
        );

        rabbitTemplate.convertAndSend(
                messagingProperties.getTicketCreatedExchange(),
                messagingProperties.getTicketCreatedRoutingKey(),
                event
        );

        log.info(
                "ticket_created_event_published exchange={} routingKey={} ticketId={} tenantId={} requestId={}",
                messagingProperties.getTicketCreatedExchange(),
                messagingProperties.getTicketCreatedRoutingKey(),
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getCreatedRequestId()
        );
    }

    /**
     * Truncates the question to a safe length for inclusion in the event payload.
     *
     * <p>240 characters is chosen as a reasonable excerpt length — enough for the notification
     * service to display a meaningful summary without bloating the AMQP message.</p>
     */
    private String toExcerpt(String question) {
        if (question == null) {
            return null;
        }
        // Truncate at 240 characters to keep event payloads small
        return question.length() > 240 ? question.substring(0, 240) : question;
    }

    /**
     * Immutable event payload published when a ticket is created.
     *
     * <p>Implements {@link Serializable} as required by Spring AMQP's message conversion
     * infrastructure, though in practice Jackson JSON serialisation is used at runtime.</p>
     */
    public record TicketCreatedEvent(
            String requestId,
            UUID ticketId,
            UUID tenantId,
            UUID createdByUserId,
            String createdByEmail,
            TicketOrigin origin,
            TicketStatus status,
            Double confidence,
            int sourceCount,
            String questionExcerpt,
            Instant createdAt
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
