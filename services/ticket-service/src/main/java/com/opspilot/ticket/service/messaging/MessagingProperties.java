package com.opspilot.ticket.service.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticket.messaging")
public class MessagingProperties {

    private boolean enabled = true;
    private String ticketCreatedExchange = "opspilot.events";
    private String ticketCreatedRoutingKey = "ticket.created";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTicketCreatedExchange() {
        return ticketCreatedExchange;
    }

    public void setTicketCreatedExchange(String ticketCreatedExchange) {
        this.ticketCreatedExchange = ticketCreatedExchange;
    }

    public String getTicketCreatedRoutingKey() {
        return ticketCreatedRoutingKey;
    }

    public void setTicketCreatedRoutingKey(String ticketCreatedRoutingKey) {
        this.ticketCreatedRoutingKey = ticketCreatedRoutingKey;
    }
}
