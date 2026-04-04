package com.opspilot.apigateway.util.logging;

import java.util.UUID;

/**
 * Utility constants and helpers for the {@code X-Request-Id} correlation ID header.
 *
 * <p>A correlation ID is a short string (UUID) that is attached to every request at the
 * gateway edge and propagated to all downstream services via the {@code X-Request-Id}
 * HTTP header. Including it in every log line makes it possible to reconstruct the full
 * end-to-end trace of a single request across multiple services in a log aggregator.
 */
public final class RequestCorrelation {

    /** The HTTP header name used to carry the correlation ID through the system. */
    public static final String HEADER_NAME = "X-Request-Id";

    private RequestCorrelation() {
        // Utility class — not instantiable
    }

    /**
     * Returns the supplied request ID if it is non-null and non-blank; otherwise
     * generates a fresh random UUID string.
     *
     * <p>This method is the single point where correlation IDs enter the system.
     * Both {@link CorrelationIdWebFilter} and {@link GatewayRequestLoggingFilter}
     * call it so that a valid ID is always available, even when the upstream client
     * does not supply one.
     *
     * @param requestId the value from the incoming {@code X-Request-Id} header,
     *                  or {@code null} if the header was absent
     * @return the original {@code requestId} if valid, or a newly generated UUID string
     */
    public static String normalizeOrGenerate(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
