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

/**
 * RabbitMQ listener that consumes domain events and forwards them to the configured webhook.
 *
 * <p>Two event types are handled: {@code ticket.created} (from the ticket-service) and
 * {@code document.processed} (from the assistant-service). For each event the listener
 * populates the MDC correlation ID from the event's {@code requestId} field, wraps the
 * payload in a {@link NotificationEnvelope}, and delegates HTTP delivery to
 * {@link WebhookDeliveryService}.</p>
 *
 * <p>If {@code notification.messaging.enabled} is {@code false} all messages are silently
 * dropped, which is useful in environments where the webhook target is not available.</p>
 */
@Component
public class EventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EventNotificationListener.class);
    private final WebhookDeliveryService webhookDeliveryService;
    private final MessagingProperties messagingProperties;

    public EventNotificationListener(WebhookDeliveryService webhookDeliveryService, MessagingProperties messagingProperties) {
        this.webhookDeliveryService = webhookDeliveryService;
        this.messagingProperties = messagingProperties;
    }

    /**
     * Handles a {@code ticket.created} event delivered from the {@code notification.ticket.created} queue.
     *
     * <p>The full {@link TicketCreatedEvent} record is forwarded as the envelope payload so that
     * the webhook receiver has access to all ticket fields, including AI-confidence metadata.</p>
     *
     * @param event the deserialised ticket-created event
     */
    @RabbitListener(queues = "${notification.messaging.ticket-created-queue}")
    public void onTicketCreated(TicketCreatedEvent event) {
        if (!messagingProperties.isEnabled()) {
            return;
        }
        // Populate MDC with the correlation ID carried in the event so log lines are traceable
        withRequestId(event.requestId(), () -> {
            log.info(
                    "notification_ticket_created_received ticketId={} tenantId={} requestId={}",
                    event.ticketId(),
                    event.tenantId(),
                    event.requestId()
            );
            // Wrap the raw event in a typed envelope before forwarding to the webhook
            webhookDeliveryService.deliver(new NotificationEnvelope(
                    "ticket.created",
                    event.requestId(),
                    event.createdAt(),
                    event
            ));
        });
    }

    /**
     * Handles a {@code document.processed} event delivered from the
     * {@code notification.document.processed} queue.
     *
     * <p>Only a subset of fields (documentId, tenantId, chunkCount) is forwarded in the
     * envelope payload — the full ingestion record is not needed by webhook consumers.</p>
     *
     * @param event the deserialised document-processed event
     */
    @RabbitListener(queues = "${notification.messaging.document-processed-queue}")
    public void onDocumentProcessed(DocumentProcessedEvent event) {
        if (!messagingProperties.isEnabled()) {
            return;
        }
        // Populate MDC with the correlation ID carried in the event so log lines are traceable
        withRequestId(event.requestId(), () -> {
            log.info(
                    "notification_document_processed_received documentId={} tenantId={} requestId={}",
                    event.documentId(),
                    event.tenantId(),
                    event.requestId()
            );
            // Project only the relevant fields into the envelope payload to keep the webhook contract minimal
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

    /**
     * Runs {@code action} with the given {@code requestId} loaded into the MDC, then clears it.
     *
     * <p>Using a try/finally block guarantees the MDC key is removed even if the action throws,
     * preventing correlation ID leakage into unrelated log lines on the same thread.</p>
     *
     * @param requestId the correlation ID from the incoming event (may be null or blank)
     * @param action    the listener logic to execute within the MDC scope
     */
    private void withRequestId(String requestId, Runnable action) {
        // normalizeOrGenerate ensures a valid ID is always present in the MDC, even if the publisher omitted it
        MDC.put(RequestCorrelation.MDC_KEY, RequestCorrelation.normalizeOrGenerate(requestId));
        try {
            action.run();
        } finally {
            MDC.remove(RequestCorrelation.MDC_KEY);
        }
    }
}
