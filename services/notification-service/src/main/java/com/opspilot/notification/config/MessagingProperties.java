package com.opspilot.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.messaging")
public class MessagingProperties {

    private boolean enabled = true;
    private String exchange = "opspilot.events";
    private String ticketCreatedRoutingKey = "ticket.created";
    private String documentProcessedRoutingKey = "document.processed";
    private String ticketCreatedQueue = "notification.ticket.created";
    private String documentProcessedQueue = "notification.document.processed";

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
}
