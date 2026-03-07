package com.opspilot.apigateway.util.logging;

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayRequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayRequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest requestWithCorrelation = exchange.getRequest();
        String requestId = RequestCorrelation.normalizeOrGenerate(
                requestWithCorrelation.getHeaders().getFirst(RequestCorrelation.HEADER_NAME)
        );

        long startedAtNanos = System.nanoTime();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        return chain.filter(exchange)
                .doOnError(errorRef::set)
                .doFinally(signalType -> {
                    long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
                    int status = exchange.getResponse().getStatusCode() == null
                            ? HttpStatus.INTERNAL_SERVER_ERROR.value()
                            : exchange.getResponse().getStatusCode().value();

                    Throwable error = errorRef.get();
                    if (error == null) {
                        log.info(
                                "gateway_request_completed method={} path={} status={} durationMs={} requestId={}",
                                requestWithCorrelation.getMethod(),
                                requestWithCorrelation.getPath().value(),
                                status,
                                durationMs,
                                requestId
                        );
                    } else {
                        log.error(
                                "gateway_request_failed method={} path={} status={} durationMs={} requestId={} message={}",
                                requestWithCorrelation.getMethod(),
                                requestWithCorrelation.getPath().value(),
                                status,
                                durationMs,
                                requestId,
                                error.getMessage(),
                                error
                        );
                    }
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
