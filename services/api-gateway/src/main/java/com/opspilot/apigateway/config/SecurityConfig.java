package com.opspilot.apigateway.config;

import com.opspilot.apigateway.logging.RequestCorrelation;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {
                })
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/auth/**", "/actuator/health", "/actuator/info").permitAll()
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
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }
}
