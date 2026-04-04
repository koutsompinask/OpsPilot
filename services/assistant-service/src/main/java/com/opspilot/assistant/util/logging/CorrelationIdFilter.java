package com.opspilot.assistant.util.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that establishes the request correlation ID for every inbound HTTP request.
 *
 * If the incoming request carries an {@code X-Request-Id} header (forwarded by the API gateway),
 * that value is reused; otherwise a new UUID is generated. The ID is written into MDC so that
 * all log statements within the request are automatically tagged with it, and it is echoed back
 * in the response header for client-side tracing. The MDC entry is cleaned up in a {@code finally}
 * block to prevent leakage across thread-pooled requests.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = RequestCorrelation.normalizeOrGenerate(request.getHeader(RequestCorrelation.HEADER_NAME));
        response.setHeader(RequestCorrelation.HEADER_NAME, requestId);
        MDC.put(RequestCorrelation.MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestCorrelation.MDC_KEY);
        }
    }
}
