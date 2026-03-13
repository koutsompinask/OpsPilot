package com.opspilot.notification.service;

import com.opspilot.notification.config.MessagingProperties;
import com.opspilot.notification.dto.DocumentProcessedEvent;
import com.opspilot.notification.dto.NotificationEnvelope;
import com.opspilot.notification.dto.TicketCreatedEvent;
import com.opspilot.notification.util.logging.RequestCorrelation;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EventNotificationListener.class);
    private final WebhookDeliveryService webhookDeliveryService;
    private final MessagingProperties messagingProperties;

    public EventNotificationListener(WebhookDeliveryService webhookDeliveryService, MessagingProperties messagingProperties) {
        this.webhookDeliveryService = webhookDeliveryService;
        this.messagingProperties = messagingProperties;
    }

    @RabbitListener(queues = "${notification.messaging.ticket-created-queue}")
    public void onTicketCreated(TicketCreatedEvent event) {
        if (!messagingProperties.isEnabled()) {
            return;
        }
        withRequestId(event.requestId(), () -> {
            log.info(
                    "notification_ticket_created_received ticketId={} tenantId={} requestId={}",
                    event.ticketId(),
                    event.tenantId(),
                    event.requestId()
            );
            webhookDeliveryService.deliver(new NotificationEnvelope(
                    "ticket.created",
                    event.requestId(),
                    event.createdAt(),
                    event
            ));
        });
    }

    @RabbitListener(queues = "${notification.messaging.document-processed-queue}")
    public void onDocumentProcessed(DocumentProcessedEvent event) {
        if (!messagingProperties.isEnabled()) {
            return;
        }
        withRequestId(event.requestId(), () -> {
            log.info(
                    "notification_document_processed_received documentId={} tenantId={} requestId={}",
                    event.documentId(),
                    event.tenantId(),
                    event.requestId()
            );
            webhookDeliveryService.deliver(new NotificationEnvelope(
                    "document.processed",
                    event.requestId(),
                    event.processedAt(),
                    Map.of(
                            "documentId", event.documentId(),
                            "tenantId", event.tenantId(),
                            "chunkCount", event.chunkCount()
                    )
            ));
        });
    }

    private void withRequestId(String requestId, Runnable action) {
        MDC.put(RequestCorrelation.MDC_KEY, RequestCorrelation.normalizeOrGenerate(requestId));
        try {
            action.run();
        } finally {
            MDC.remove(RequestCorrelation.MDC_KEY);
        }
    }
}
