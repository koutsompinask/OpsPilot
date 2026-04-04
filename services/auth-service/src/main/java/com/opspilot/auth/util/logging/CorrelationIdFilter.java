package com.opspilot.auth.util.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that extracts or generates a correlation ID for each inbound HTTP request and
 * makes it available for structured logging via the SLF4J MDC.
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee single execution per request (including
 * forwarded and error-dispatched requests). The correlation ID is echoed back to the caller on
 * the {@code X-Request-Id} response header so that clients can correlate their requests with
 * server-side log entries.</p>
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    /**
     * Resolves the correlation ID, binds it to the MDC, and ensures it is removed after the
     * request completes to prevent MDC leakage across pooled threads.
     *
     * @param request     the current HTTP request
     * @param response    the current HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if the filter chain throws a servlet error
     * @throws IOException      if an I/O error occurs during filter processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = RequestCorrelation.normalizeOrGenerate(request.getHeader(RequestCorrelation.HEADER_NAME));
        // Echo the ID on the response so callers can correlate logs end-to-end
        response.setHeader(RequestCorrelation.HEADER_NAME, requestId);
        MDC.put(RequestCorrelation.MDC_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always remove from MDC to prevent leakage to subsequent requests on the same thread
            MDC.remove(RequestCorrelation.MDC_KEY);
        }
    }
}
