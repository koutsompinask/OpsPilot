package com.opspilot.apigateway.util.logging;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = RequestCorrelation.normalizeOrGenerate(
                exchange.getRequest().getHeaders().getFirst(RequestCorrelation.HEADER_NAME)
        );

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(RequestCorrelation.HEADER_NAME, requestId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getResponse().getHeaders().set(RequestCorrelation.HEADER_NAME, requestId);

        return chain.filter(mutatedExchange);
    }
}
