package com.opspilot.assistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the assistant-service.
 *
 * <p>Configures the service as a stateless OAuth2 JWT resource server. All requests
 * require a valid Bearer JWT issued by the auth-service, except for the Actuator
 * health and info endpoints which are left open for infrastructure health checks.
 * CSRF protection is disabled because the service is stateless and does not use
 * cookie-based sessions.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Defines the security filter chain applied to all incoming HTTP requests.
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the fully configured {@link SecurityFilterChain}
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless API — no CSRF risk without session cookies
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Permit liveness/readiness probes without authentication
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                // Validate incoming JWTs using the configured spring.security.oauth2.resourceserver.jwt.* properties
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));
        return http.build();
    }
}
