package com.opspilot.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ticket-service.
 *
 * <p>The ticket-service manages support tickets within OpsPilot. Tickets may be created manually
 * by tenant admins or auto-escalated by the assistant-service when a chat response falls below the
 * configured confidence threshold. The service publishes {@code ticket.created} events to RabbitMQ
 * so that the notification-service can deliver webhooks to interested subscribers.</p>
 */
@SpringBootApplication
public class TicketServiceApplication {

    /**
     * Bootstraps the Spring Boot application context and starts the embedded server.
     *
     * @param args command-line arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}