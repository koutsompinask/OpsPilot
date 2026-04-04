package com.opspilot.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the ticket-service.
 *
 * <p>The service operates as a stateless JWT resource server. Public routes are limited to
 * internal service-to-service endpoints (authenticated via a shared service token at the
 * application layer) and actuator health/info probes. All other routes require a valid
 * JWT bearer token issued by the auth-service.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Defines the security filter chain applied to every HTTP request.
     *
     * <p>Notable decisions:
     * <ul>
     *   <li>CSRF is disabled — the service is stateless and does not use session cookies.</li>
     *   <li>{@code /internal/**} routes are permitted at the Spring Security layer because they
     *       are protected at the application layer via the {@code X-Service-Token} header check
     *       in {@link com.opspilot.ticket.controller.InternalTicketController}.</li>
     *   <li>JWT decoder bean is provided by {@link JwtConfig}.</li>
     * </ul>
     * </p>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if the security configuration cannot be applied
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Internal endpoints use service-token auth (checked in the controller),
                        // and actuator probes must remain unauthenticated for health checks
                        .requestMatchers("/internal/**", "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));
        return http.build();
    }
}
