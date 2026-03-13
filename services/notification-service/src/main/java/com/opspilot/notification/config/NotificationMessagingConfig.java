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

@Configuration
@EnableRabbit
@EnableConfigurationProperties({MessagingProperties.class, WebhookProperties.class})
public class NotificationMessagingConfig {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange notificationExchange(MessagingProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue ticketCreatedQueue(MessagingProperties properties) {
        return new Queue(properties.getTicketCreatedQueue(), true);
    }

    @Bean
    public Queue documentProcessedQueue(MessagingProperties properties) {
        return new Queue(properties.getDocumentProcessedQueue(), true);
    }

    @Bean
    public Binding ticketCreatedBinding(Queue ticketCreatedQueue, DirectExchange notificationExchange, MessagingProperties properties) {
        return BindingBuilder.bind(ticketCreatedQueue)
                .to(notificationExchange)
                .with(properties.getTicketCreatedRoutingKey());
    }

    @Bean
    public Binding documentProcessedBinding(Queue documentProcessedQueue, DirectExchange notificationExchange, MessagingProperties properties) {
        return BindingBuilder.bind(documentProcessedQueue)
                .to(notificationExchange)
                .with(properties.getDocumentProcessedRoutingKey());
    }
}
