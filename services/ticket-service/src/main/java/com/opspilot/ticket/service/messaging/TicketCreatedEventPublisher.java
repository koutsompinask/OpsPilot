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

@Component
public class TicketCreatedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TicketCreatedEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messagingProperties;

    public TicketCreatedEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties messagingProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingProperties = messagingProperties;
    }

    public void publish(Ticket ticket) {
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

    private String toExcerpt(String question) {
        if (question == null) {
            return null;
        }
        return question.length() > 240 ? question.substring(0, 240) : question;
    }

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
