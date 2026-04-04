package com.opspilot.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

/**
 * RabbitMQ topology and message-converter configuration for the notification-service.
 *
 * <p>Declares the shared {@code opspilot.events} direct exchange together with two durable,
 * service-owned queues — one per consumed event type. Each queue is bound to the exchange with
 * its own routing key so that messages published by other services are routed exclusively to
 * the correct queue. Using separate queues per event type keeps consumer throughput independent
 * and makes it straightforward to apply per-queue dead-lettering in the future.</p>
 *
 * <p>All beans are idempotent: RabbitMQ will verify that the declared exchange and queues
 * already exist with matching arguments and leave them unchanged if they do.</p>
 */
@Configuration
@EnableRabbit
@EnableConfigurationProperties({MessagingProperties.class, WebhookProperties.class})
public class NotificationMessagingConfig {

    /**
     * Registers a Jackson-based message converter so that AMQP message bodies are automatically
     * deserialised from JSON into the target event record types.
     *
     * @return a {@link Jackson2JsonMessageConverter} backed by the default {@code ObjectMapper}
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Declares the shared direct exchange that all OpsPilot services publish events to.
     *
     * <p>The exchange is durable ({@code true}) and non-auto-delete ({@code false}) so it
     * survives broker restarts and is not removed when the last consumer disconnects.</p>
     *
     * @param properties messaging configuration holding the exchange name
     * @return a durable {@link DirectExchange} named after the configured exchange
     */
    @Bean
    public DirectExchange notificationExchange(MessagingProperties properties) {
        // durable=true so the exchange survives broker restarts; autoDelete=false keeps it alive when idle
        return new DirectExchange(properties.getExchange(), true, false);
    }

    /**
     * Declares the durable queue that receives {@code ticket.created} events.
     *
     * @param properties messaging configuration holding the queue name
     * @return a durable {@link Queue} dedicated to ticket-created notifications
     */
    @Bean
    public Queue ticketCreatedQueue(MessagingProperties properties) {
        // Durable queue so messages survive a broker restart
        return new Queue(properties.getTicketCreatedQueue(), true);
    }

    /**
     * Declares the durable queue that receives {@code document.processed} events.
     *
     * @param properties messaging configuration holding the queue name
     * @return a durable {@link Queue} dedicated to document-processed notifications
     */
    @Bean
    public Queue documentProcessedQueue(MessagingProperties properties) {
        // Durable queue so messages survive a broker restart
        return new Queue(properties.getDocumentProcessedQueue(), true);
    }

    /**
     * Binds the ticket-created queue to the shared exchange using the {@code ticket.created} routing key.
     *
     * <p>Only messages published with exactly this routing key will be delivered to the queue.</p>
     *
     * @param ticketCreatedQueue    the queue declared by {@link #ticketCreatedQueue}
     * @param notificationExchange  the shared direct exchange
     * @param properties            messaging configuration holding the routing key
     * @return the {@link Binding} between the queue and the exchange
     */
    @Bean
    public Binding ticketCreatedBinding(Queue ticketCreatedQueue, DirectExchange notificationExchange, MessagingProperties properties) {
        return BindingBuilder.bind(ticketCreatedQueue)
                .to(notificationExchange)
                .with(properties.getTicketCreatedRoutingKey());
    }

    /**
     * Binds the document-processed queue to the shared exchange using the {@code document.processed} routing key.
     *
     * <p>Only messages published with exactly this routing key will be delivered to the queue.</p>
     *
     * @param documentProcessedQueue the queue declared by {@link #documentProcessedQueue}
     * @param notificationExchange   the shared direct exchange
     * @param properties             messaging configuration holding the routing key
     * @return the {@link Binding} between the queue and the exchange
     */
    @Bean
    public Binding documentProcessedBinding(Queue documentProcessedQueue, DirectExchange notificationExchange, MessagingProperties properties) {
        return BindingBuilder.bind(documentProcessedQueue)
                .to(notificationExchange)
                .with(properties.getDocumentProcessedRoutingKey());
    }
}
