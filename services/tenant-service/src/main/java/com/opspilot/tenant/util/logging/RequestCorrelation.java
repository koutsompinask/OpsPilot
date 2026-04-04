package com.opspilot.tenant.util.logging;

import java.util.UUID;

/**
 * Constants and utilities for request correlation ID propagation.
 *
 * <p>A correlation ID is attached to every inbound HTTP request and stored in the SLF4J MDC
 * under the key {@value #MDC_KEY}. Outbound calls to auth-service forward the same ID via the
 * {@value #HEADER_NAME} header so that a single user action can be traced across service logs.
 * If no ID is present on an inbound request, a new UUID is generated to ensure every log entry
 * can be tied to exactly one request.
 */
public final class RequestCorrelation {

    /** HTTP header name used to carry the correlation ID between services. */
    public static final String HEADER_NAME = "X-Request-Id";
    /** SLF4J MDC key under which the correlation ID is stored for the duration of a request. */
    public static final String MDC_KEY = "requestId";

    private RequestCorrelation() {
    }

    /**
     * Returns the given {@code requestId} if non-blank, otherwise generates a new random UUID string.
     *
     * @param requestId the value from the incoming {@code X-Request-Id} header, or {@code null}
     * @return a non-blank correlation ID guaranteed to be set for every request
     */
    public static String normalizeOrGenerate(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            // No ID provided by the upstream caller; generate one so every log line is traceable
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
