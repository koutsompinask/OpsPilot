package com.opspilot.assistant.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures the asynchronous thread pool used for document ingestion tasks.
 */
@Configuration
public class AsyncConfig {

    /**
     * Creates a bounded thread pool dedicated to document ingestion.
     *
     * <p>Core size is kept small (2) to avoid overwhelming the embedding provider
     * with concurrent requests; max size of 4 allows short bursts. The queue capacity
     * of 100 prevents request loss under moderate load without unbounded growth.</p>
     *
     * @return the configured {@link Executor} registered under the name {@code ingestionExecutor}
     */
    @Bean(name = "ingestionExecutor")
    public Executor ingestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ingestion-");
        executor.setCorePoolSize(2);   // 2 concurrent ingestion workers by default
        executor.setMaxPoolSize(4);    // burst up to 4 under load
        executor.setQueueCapacity(100); // buffer up to 100 pending ingestion tasks
        executor.initialize();
        return executor;
    }
}
