package com.opspilot.assistant.service.retrieval;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for the reranking subsystem.
 *
 * <p>Binds {@link RerankerProperties} and creates a dedicated {@link RestTemplate}
 * with a configurable timeout. Reranking calls can be slower than embedding calls
 * (cross-encoder models are more expensive), so the timeout is configured separately.</p>
 */
@Configuration
@EnableConfigurationProperties(RerankerProperties.class)
public class RerankerConfig {

    /**
     * Creates a {@link RestTemplate} with connect and read timeouts driven by
     * {@code assistant.reranker.request-timeout-ms}.
     *
     * @param properties the reranker configuration properties
     * @return a timeout-bounded {@link RestTemplate} for reranker HTTP calls
     */
    @Bean(name = "rerankerRestTemplate")
    public RestTemplate rerankerRestTemplate(RerankerProperties properties) {
        Duration timeout = Duration.ofMillis(properties.getRequestTimeoutMs());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }
}
