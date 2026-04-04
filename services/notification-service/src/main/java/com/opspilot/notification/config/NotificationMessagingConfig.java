package com.opspilot.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
 * the correct queue. Using separate queues per event type keeps consumer throughput independent.</p>
 *
 * <p>Dead-letter exchange (DLX): each main queue is configured with {@code x-dead-letter-exchange}
 * pointing to a fanout DLX ({@code opspilot.events.dlx}). Messages that are rejected, expired,
 * or overflow the queue are routed to the DLX and land in dedicated DLQs for operator inspection
 * rather than being silently dropped.</p>
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
     * Declares the dead-letter exchange (DLX) that receives undeliverable messages from the main queues.
     *
     * <p>A fanout exchange is used so that all dead-lettered messages are routed to every bound DLQ
     * without requiring a specific routing key. This simplifies the DLX topology.</p>
     *
     * @param properties messaging configuration holding the DLX name
     * @return a durable {@link FanoutExchange} used as the dead-letter destination
     */
    @Bean
    public FanoutExchange deadLetterExchange(MessagingProperties properties) {
        return new FanoutExchange(properties.getDeadLetterExchange(), true, false);
    }

    /**
     * Declares the dead-letter queue for {@code ticket.created} events.
     *
     * <p>Messages rejected or expired from the main ticket-created queue land here for
     * operator inspection and manual replay.</p>
     *
     * @param properties messaging configuration holding the DLQ name
     * @return a durable DLQ bound via the DLX
     */
    @Bean
    public Queue ticketCreatedDlq(MessagingProperties properties) {
        return QueueBuilder.durable(properties.getTicketCreatedDlq()).build();
    }

    /**
     * Declares the dead-letter queue for {@code document.processed} events.
     *
     * @param properties messaging configuration holding the DLQ name
     * @return a durable DLQ bound via the DLX
     */
    @Bean
    public Queue documentProcessedDlq(MessagingProperties properties) {
        return QueueBuilder.durable(properties.getDocumentProcessedDlq()).build();
    }

    /**
     * Binds the ticket-created DLQ to the dead-letter exchange.
     *
     * @param ticketCreatedDlq    the DLQ declared by {@link #ticketCreatedDlq}
     * @param deadLetterExchange  the DLX declared by {@link #deadLetterExchange}
     * @return the fanout binding
     */
    @Bean
    public Binding ticketCreatedDlqBinding(Queue ticketCreatedDlq, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(ticketCreatedDlq).to(deadLetterExchange);
    }

    /**
     * Binds the document-processed DLQ to the dead-letter exchange.
     *
     * @param documentProcessedDlq the DLQ declared by {@link #documentProcessedDlq}
     * @param deadLetterExchange   the DLX declared by {@link #deadLetterExchange}
     * @return the fanout binding
     */
    @Bean
    public Binding documentProcessedDlqBinding(Queue documentProcessedDlq, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(documentProcessedDlq).to(deadLetterExchange);
    }

    /**
     * Declares the durable queue that receives {@code ticket.created} events.
     *
     * <p>The queue is configured with {@code x-dead-letter-exchange} so that rejected or
     * expired messages are automatically routed to the DLX instead of being silently dropped.</p>
     *
     * @param properties messaging configuration holding the queue and DLX names
     * @return a durable {@link Queue} with dead-letter routing configured
     */
    @Bean
    public Queue ticketCreatedQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.getTicketCreatedQueue())
                // Route rejected/expired messages to the DLX rather than dropping them silently
                .withArgument("x-dead-letter-exchange", properties.getDeadLetterExchange())
                .build();
    }

    /**
     * Declares the durable queue that receives {@code document.processed} events.
     *
     * <p>The queue is configured with {@code x-dead-letter-exchange} so that rejected or
     * expired messages are automatically routed to the DLX instead of being silently dropped.</p>
     *
     * @param properties messaging configuration holding the queue and DLX names
     * @return a durable {@link Queue} with dead-letter routing configured
     */
    @Bean
    public Queue documentProcessedQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.getDocumentProcessedQueue())
                // Route rejected/expired messages to the DLX rather than dropping them silently
                .withArgument("x-dead-letter-exchange", properties.getDeadLetterExchange())
                .build();
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
