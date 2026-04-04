package com.opspilot.auth.config;

import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the auth-service.
 *
 * <p>All {@code /auth/**} and {@code /internal/**} endpoints are publicly accessible because
 * the auth-service is itself the credential issuer — it cannot validate a JWT before one has
 * been issued. Actuator health and info probes are similarly open for liveness checks.
 * The service runs fully stateless (no HTTP sessions); token state is managed via
 * {@link com.opspilot.auth.domain.entity.RefreshSession} records in the database.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the HTTP security filter chain.
     *
     * <p>CSRF protection is disabled because all clients are stateless API consumers that do
     * not rely on browser cookies. Session creation is set to {@code STATELESS} to prevent
     * Spring Security from creating or consulting an {@code HttpSession}.</p>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the assembled {@link SecurityFilterChain}
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Public: auth flows, internal service endpoints, and actuator probes
                        .requestMatchers("/auth/**", "/internal/**", "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Provides a {@link BCryptPasswordEncoder} used only for the BCrypt layer inside
     * {@link com.opspilot.auth.service.PasswordHashService}.
     *
     * @return a {@link PasswordEncoder} backed by BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides a cryptographically strong {@link SecureRandom} instance shared across the
     * application (primarily by {@link com.opspilot.auth.service.PasswordHashService} for
     * salt generation).
     *
     * @return a {@link SecureRandom} seeded from the platform entropy source
     */
    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
