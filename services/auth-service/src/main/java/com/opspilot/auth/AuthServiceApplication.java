package com.opspilot.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the auth-service.
 *
 * <p>The auth-service is responsible for tenant registration, user login, JWT access token
 * issuance, refresh token session management, and internal user creation on behalf of the
 * tenant-service. It owns the {@code auth} schema and is the sole authority for credential
 * storage and validation.</p>
 */
@SpringBootApplication
public class AuthServiceApplication {

    /**
     * Bootstraps and launches the Spring Boot application.
     *
     * @param args command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}