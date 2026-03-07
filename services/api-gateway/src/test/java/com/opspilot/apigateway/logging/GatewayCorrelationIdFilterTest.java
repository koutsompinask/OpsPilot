package com.opspilot.apigateway.logging;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayCorrelationIdFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldGenerateRequestIdWhenMissing() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(RequestCorrelation.HEADER_NAME);
    }

    @Test
    void shouldPreserveRequestIdWhenProvided() {
        String requestId = "gateway-test-request-id";

        webTestClient.get()
                .uri("/actuator/health")
                .header(RequestCorrelation.HEADER_NAME, requestId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(RequestCorrelation.HEADER_NAME, requestId);
    }
}
