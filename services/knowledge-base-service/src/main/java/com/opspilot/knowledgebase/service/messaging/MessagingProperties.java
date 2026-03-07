package com.opspilot.knowledgebase.service.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge.messaging")
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
