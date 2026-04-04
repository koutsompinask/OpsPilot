package com.opspilot.tenant.util.logging;

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
 * Reads the {@code X-Request-Id} header forwarded by the API gateway; generates a new UUID
 * if absent. Writes the ID into MDC so all log statements within the request are tagged,
 * and echoes it in the response header. Cleans up MDC in a {@code finally} block.
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
