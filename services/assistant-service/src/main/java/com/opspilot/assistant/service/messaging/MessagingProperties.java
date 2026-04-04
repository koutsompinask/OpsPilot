package com.opspilot.assistant.service.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for RabbitMQ event publishing in the assistant-service,
 * bound from the {@code assistant.messaging.*} namespace.
 *
 * The {@code enabled} flag allows event publishing to be disabled in test environments
 * without requiring a running RabbitMQ broker.
 */
@ConfigurationProperties(prefix = "assistant.messaging")
public class MessagingProperties {

    private boolean enabled = true;
    private String documentProcessedExchange = "opspilot.events";
    private String documentProcessedRoutingKey = "document.processed";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDocumentProcessedExchange() {
        return documentProcessedExchange;
    }

    public void setDocumentProcessedExchange(String documentProcessedExchange) {
        this.documentProcessedExchange = documentProcessedExchange;
    }

    public String getDocumentProcessedRoutingKey() {
        return documentProcessedRoutingKey;
    }

    public void setDocumentProcessedRoutingKey(String documentProcessedRoutingKey) {
        this.documentProcessedRoutingKey = documentProcessedRoutingKey;
    }
}
