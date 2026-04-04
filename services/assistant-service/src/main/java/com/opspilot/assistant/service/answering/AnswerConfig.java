package com.opspilot.assistant.service.answering;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for the answer-generation layer.
 *
 * Provisions a dedicated {@link RestTemplate} bean for LLM HTTP calls, configured with
 * the request timeout from {@code ai.answer.request-timeout-ms}.
 */
@Configuration
@EnableConfigurationProperties(AnswerProperties.class)
public class AnswerConfig {

    /**
     * Creates a {@link RestTemplate} for LLM API calls with the configured timeout applied
     * to both the connect and read phases.
     *
     * @param properties answer configuration bound from {@code ai.answer.*}
     * @return a timeout-configured RestTemplate
     */
    @Bean(name = "answerRestTemplate")
    public RestTemplate answerRestTemplate(AnswerProperties properties) {
        Duration timeout = Duration.ofMillis(properties.getRequestTimeoutMs());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }
}
