package com.opspilot.tenant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the tenant-service.
 *
 * <p>Configures a stateless JWT resource-server. The {@code /internal/**} prefix is intentionally
 * left open to JWT validation — it is instead guarded by a shared service token checked in
 * {@link com.opspilot.tenant.controller.InternalTenantController}. Actuator health and info
 * endpoints are exposed without authentication to support container health checks.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Defines the security filter chain applied to all incoming HTTP requests.
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if the configuration cannot be applied
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection is unnecessary for a stateless REST API that uses Bearer tokens
                .csrf(csrf -> csrf.disable())
                // No server-side session state — each request must carry a valid JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Internal service-to-service endpoints use a shared token, not a user JWT
                        .requestMatchers("/internal/**", "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                // Delegate JWT validation to the JwtDecoder bean defined in JwtConfig
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));
        return http.build();
    }
}
