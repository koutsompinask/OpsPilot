package com.opspilot.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the OpsPilot API Gateway service.
 *
 * The API Gateway is the single ingress point for all frontend traffic. It uses
 * Spring Cloud Gateway to route requests to downstream microservices, validates
 * JWTs as an OAuth2 resource server, and generates/propagates {@code X-Request-Id}
 * correlation IDs across every request.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Bootstraps the Spring application context and starts the embedded server.
     *
     * @param args command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}