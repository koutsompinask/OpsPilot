package com.opspilot.apigateway.util.logging;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive web filter responsible for the correlation ID lifecycle on every request.
 *
 * <p>Correlation ID lifecycle:
 * <ol>
 *   <li><b>Inbound:</b> If the client sends an {@code X-Request-Id} header the value is
 *       reused (normalised via {@link RequestCorrelation#normalizeOrGenerate}); otherwise
 *       a new UUID is generated. This allows callers — such as integration tests or API
 *       clients — to inject their own trace IDs.</li>
 *   <li><b>Downstream propagation:</b> The ID is written into the mutated outbound request
 *       so that every downstream service receives it as a request header and can include
 *       it in their own logs.</li>
 *   <li><b>Response echo:</b> The same ID is also set on the response headers so the
 *       browser/client can correlate its own logs with server-side log entries.</li>
 * </ol>
 *
 * <p>This filter runs at {@link Ordered#HIGHEST_PRECEDENCE} to ensure the correlation ID
 * is present on the exchange before any other filter (including security filters and the
 * {@link GatewayRequestLoggingFilter}) reads it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    /**
     * Ensures the {@code X-Request-Id} correlation ID is present on both the forwarded
     * request and the response, then delegates to the rest of the filter chain.
     *
     * @param exchange the current server web exchange
     * @param chain    the remaining filter chain to delegate to
     * @return a {@link Mono} that completes when the full filter chain has processed the exchange
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Reuse the client-supplied ID if present; generate a new UUID if absent or blank
        String requestId = RequestCorrelation.normalizeOrGenerate(
                exchange.getRequest().getHeaders().getFirst(RequestCorrelation.HEADER_NAME)
        );

        // Mutate the request to inject/overwrite the header so downstream services
        // always receive a well-formed correlation ID regardless of what the client sent
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(RequestCorrelation.HEADER_NAME, requestId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        // Echo the correlation ID back in the response so clients can match their
        // outbound request to server-side log entries
        mutatedExchange.getResponse().getHeaders().set(RequestCorrelation.HEADER_NAME, requestId);

        return chain.filter(mutatedExchange);
    }
}
