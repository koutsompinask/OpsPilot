package com.opspilot.assistant.service.messaging;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the assistant-service.
 *
 * Registers a {@link Jackson2JsonMessageConverter} so that event payloads are
 * serialised/deserialised as JSON rather than Java serialisation.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingConfig {

    /**
     * Configures JSON message conversion for all {@link org.springframework.amqp.rabbit.core.RabbitTemplate} operations.
     *
     * @return a Jackson-based message converter
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
