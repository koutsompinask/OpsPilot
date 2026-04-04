package com.opspilot.assistant.service.embedding;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for the embedding subsystem.
 *
 * <p>Binds {@link EmbeddingProperties} and creates a dedicated {@link RestTemplate}
 * with configurable connect/read timeouts so that slow embedding providers do not
 * block the ingestion pipeline indefinitely.</p>
 */
@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingConfig {

    /**
     * Creates a {@link RestTemplate} with connect and read timeouts driven by
     * {@code assistant.embedding.request-timeout-ms}.
     *
     * @param properties the embedding configuration properties
     * @return a timeout-bounded {@link RestTemplate} for embedding HTTP calls
     */
    @Bean(name = "embeddingRestTemplate")
    public RestTemplate embeddingRestTemplate(EmbeddingProperties properties) {
        Duration timeout = Duration.ofMillis(properties.getRequestTimeoutMs());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }
}
