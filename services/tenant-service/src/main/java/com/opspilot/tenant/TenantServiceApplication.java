package com.opspilot.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the tenant-service.
 *
 * <p>This service manages tenant profiles and tenant-scoped user accounts. It acts as the
 * multi-tenancy backbone: every other service derives tenant context from the JWT claims that
 * this service's user accounts produce. It also owns the tenant bootstrap flow, where
 * auth-service calls the internal endpoint to create the initial tenant record and admin profile
 * after a new tenant registers.
 */
@SpringBootApplication
public class TenantServiceApplication {

    /**
     * Starts the tenant-service Spring Boot application.
     *
     * @param args command-line arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(TenantServiceApplication.class, args);
    }
}