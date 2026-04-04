package com.opspilot.ticket.service.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalisable configuration properties for RabbitMQ messaging in the ticket-service.
 *
 * <p>All properties are bound from the {@code ticket.messaging.*} namespace. Setting
 * {@code ticket.messaging.enabled=false} suppresses all event publishing, which is useful
 * in local development environments where RabbitMQ is not available.</p>
 */
@ConfigurationProperties(prefix = "ticket.messaging")
public class MessagingProperties {

    /** Whether event publishing is active. Defaults to {@code true}. */
    private boolean enabled = true;

    /** Name of the RabbitMQ direct exchange that receives ticket events. Defaults to {@code opspilot.events}. */
    private String ticketCreatedExchange = "opspilot.events";

    /** Routing key used when publishing {@code ticket.created} events. Defaults to {@code ticket.created}. */
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
