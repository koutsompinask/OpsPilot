package com.opspilot.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the RabbitMQ messaging topology used by the notification-service.
 *
 * <p>Properties are bound from the {@code notification.messaging.*} namespace. Defaults reflect
 * the shared {@code opspilot.events} direct exchange and the two dedicated queues consumed by
 * this service: {@code notification.ticket.created} and {@code notification.document.processed}.
 * Setting {@code enabled = false} causes all incoming messages to be silently dropped, which is
 * useful in environments where RabbitMQ is unavailable (e.g., local development without Docker).</p>
 */
@ConfigurationProperties(prefix = "notification.messaging")
public class MessagingProperties {

    // When false, the listener methods return immediately without forwarding to the webhook
    private boolean enabled = true;
    private String exchange = "opspilot.events";
    // Routing keys must match the keys used by the publishing services (ticket-service, assistant-service)
    private String ticketCreatedRoutingKey = "ticket.created";
    private String documentProcessedRoutingKey = "document.processed";
    private String ticketCreatedQueue = "notification.ticket.created";
    private String documentProcessedQueue = "notification.document.processed";
    // Dead-letter exchange: undeliverable or failed messages are routed here instead of being lost
    private String deadLetterExchange = "opspilot.events.dlx";
    private String ticketCreatedDlq = "notification.ticket.created.dlq";
    private String documentProcessedDlq = "notification.document.processed.dlq";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getTicketCreatedRoutingKey() {
        return ticketCreatedRoutingKey;
    }

    public void setTicketCreatedRoutingKey(String ticketCreatedRoutingKey) {
        this.ticketCreatedRoutingKey = ticketCreatedRoutingKey;
    }

    public String getDocumentProcessedRoutingKey() {
        return documentProcessedRoutingKey;
    }

    public void setDocumentProcessedRoutingKey(String documentProcessedRoutingKey) {
        this.documentProcessedRoutingKey = documentProcessedRoutingKey;
    }

    public String getTicketCreatedQueue() {
        return ticketCreatedQueue;
    }

    public void setTicketCreatedQueue(String ticketCreatedQueue) {
        this.ticketCreatedQueue = ticketCreatedQueue;
    }

    public String getDocumentProcessedQueue() {
        return documentProcessedQueue;
    }

    public void setDocumentProcessedQueue(String documentProcessedQueue) {
        this.documentProcessedQueue = documentProcessedQueue;
    }

    public String getDeadLetterExchange() {
        return deadLetterExchange;
    }

    public void setDeadLetterExchange(String deadLetterExchange) {
        this.deadLetterExchange = deadLetterExchange;
    }

    public String getTicketCreatedDlq() {
        return ticketCreatedDlq;
    }

    public void setTicketCreatedDlq(String ticketCreatedDlq) {
        this.ticketCreatedDlq = ticketCreatedDlq;
    }

    public String getDocumentProcessedDlq() {
        return documentProcessedDlq;
    }

    public void setDocumentProcessedDlq(String documentProcessedDlq) {
        this.documentProcessedDlq = documentProcessedDlq;
    }
}
