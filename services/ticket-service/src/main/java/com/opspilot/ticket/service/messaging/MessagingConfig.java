package com.opspilot.ticket.service.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ messaging configuration for the ticket-service.
 *
 * <p>Declares the durable direct exchange used to publish {@code ticket.created} events.
 * The notification-service binds a queue to this exchange using the {@code ticket.created}
 * routing key to consume events and deliver webhooks.</p>
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingConfig {

    /**
     * Registers a Jackson-based message converter so that event payloads are serialised as JSON
     * rather than Java serialisation, making them readable by any consumer regardless of language.
     *
     * @return a {@link Jackson2JsonMessageConverter} used by {@link org.springframework.amqp.rabbit.core.RabbitTemplate}
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Declares the durable direct exchange that receives ticket events.
     *
     * <p>The exchange is durable ({@code durable=true}) so it survives broker restarts, and
     * non-auto-delete ({@code autoDelete=false}) so it persists even when no consumers are
     * connected. Routing is done via the {@code ticket.created} routing key configured in
     * {@link MessagingProperties}.</p>
     *
     * @param properties messaging properties holding the exchange name
     * @return the configured {@link DirectExchange}
     */
    @Bean
    public DirectExchange ticketExchange(MessagingProperties properties) {
        // durable=true: exchange survives broker restart
        // autoDelete=false: exchange is not removed when the last consumer unbinds
        return new DirectExchange(properties.getTicketCreatedExchange(), true, false);
    }
}
