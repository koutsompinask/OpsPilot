package com.opspilot.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the notification-service.
 *
 * <p>The notification-service is a stateless event consumer that listens to domain events
 * published on the {@code opspilot.events} RabbitMQ exchange and forwards them as HTTP
 * POST requests to a configured webhook URL. It holds no database state; all delivery
 * outcomes are logged but not persisted or retried.</p>
 */
@SpringBootApplication
public class NotificationServiceApplication {

    /**
     * Bootstraps the Spring application context and starts the embedded server.
     *
     * @param args command-line arguments passed through to {@link SpringApplication}
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}