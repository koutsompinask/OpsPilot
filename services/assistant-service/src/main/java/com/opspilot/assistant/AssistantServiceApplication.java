package com.opspilot.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the assistant-service Spring Boot application.
 *
 * <p>This service hosts the RAG (Retrieval-Augmented Generation) pipeline for OpsPilot,
 * including document ingestion, embedding management, hybrid chunk retrieval, answer
 * generation, and low-confidence ticket escalation. {@code @EnableAsync} activates the
 * dedicated {@code ingestionExecutor} thread pool used by {@link com.opspilot.assistant.service.DocumentIngestionProcessor}
 * to process uploaded documents without blocking the HTTP request thread.</p>
 */
@SpringBootApplication
@EnableAsync
public class AssistantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantServiceApplication.class, args);
    }
}
