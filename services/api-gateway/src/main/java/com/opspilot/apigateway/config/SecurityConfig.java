package com.opspilot.apigateway.config;

import com.opspilot.apigateway.util.logging.RequestCorrelation;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configures the reactive Spring Security filter chain for the API Gateway.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Declares which route prefixes are public (no token required) vs. protected.</li>
 *   <li>Configures the gateway as a stateless OAuth2 resource server that validates
 *       JWTs on every protected request using the decoder provided by {@link JwtConfig}.</li>
 *   <li>Returns structured log entries (including the correlation ID) on authentication
 *       and authorisation failures so that rejections are observable in log aggregation.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Builds and returns the security filter chain applied to all incoming requests.
     *
     * <p>Route-level authorisation rules (evaluated top-to-bottom, first match wins):
     * <ol>
     *   <li>All {@code OPTIONS} pre-flight requests are permitted unconditionally so
     *       browsers can complete CORS handshakes before sending credentials.</li>
     *   <li>{@code /auth/**} is public — registration and login endpoints cannot
     *       require a token because the client does not have one yet.</li>
     *   <li>Actuator {@code /health} and {@code /info} are public so that container
     *       orchestrators (e.g. Kubernetes liveness/readiness probes) can poll them
     *       without needing to supply a service token.</li>
     *   <li>{@code /internal/**} is unconditionally denied — these paths are reserved for
     *       service-to-service communication and must never be reachable from the public
     *       internet, regardless of whether a valid JWT is present.</li>
     *   <li>Every other path requires a valid JWT bearer token.</li>
     * </ol>
     *
     * <p>CSRF protection is disabled because the gateway is a stateless API; it issues
     * no cookies and holds no session state, so CSRF attacks are not applicable here.
     *
     * @param http the reactive HTTP security builder provided by Spring Security
     * @return the fully configured {@link SecurityWebFilterChain}
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // No session cookies are issued, so CSRF tokens are unnecessary
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {
                })
                .authorizeExchange(exchange -> exchange
                        // Allow browser pre-flight requests through without a token
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // /auth/** is public: login and registration requests arrive without a token
                        // Actuator health/info endpoints are open for liveness and readiness probes
                        .pathMatchers("/auth/**", "/actuator/health", "/actuator/info").permitAll()
                        // /internal/** is for service-to-service calls only; deny unconditionally at the
                        // gateway so these endpoints are never reachable from the public internet even
                        // if a route accidentally exposes them
                        .pathMatchers("/internal/**").denyAll()
                        // All remaining routes require a valid JWT issued by auth-service
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, ex) -> {
                            String requestId = exchange.getRequest().getHeaders().getFirst(RequestCorrelation.HEADER_NAME);
                            log.warn(
                                    "gateway_authentication_failed method={} path={} requestId={} reason={}",
                                    exchange.getRequest().getMethod(),
                                    exchange.getRequest().getPath().value(),
                                    requestId,
                                    ex.getMessage()
                            );
                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            String requestId = exchange.getRequest().getHeaders().getFirst(RequestCorrelation.HEADER_NAME);
                            log.warn(
                                    "gateway_access_denied method={} path={} requestId={} reason={}",
                                    exchange.getRequest().getMethod(),
                                    exchange.getRequest().getPath().value(),
                                    requestId,
                                    ex.getMessage()
                            );
                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }))
                // Configure as a stateless OAuth2 resource server; JWT validation is
                // handled by the ReactiveJwtDecoder bean defined in JwtConfig
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }
}
