package com.opspilot.auth.util.logging;

import java.util.UUID;

/**
 * Constants and utilities for request correlation ID propagation.
 *
 * <p>A correlation ID is attached to every inbound HTTP request so that logs across multiple
 * services can be joined by a single identifier. If the caller supplies an {@code X-Request-Id}
 * header, its value is reused; otherwise a fresh UUID is generated. The ID is placed in the
 * SLF4J {@link org.slf4j.MDC} under the key {@value #MDC_KEY} so that all log statements
 * within the same request thread automatically include it.</p>
 */
public final class RequestCorrelation {

    /** The HTTP header name used to propagate the correlation ID between services. */
    public static final String HEADER_NAME = "X-Request-Id";

    /** The MDC key under which the correlation ID is stored for log enrichment. */
    public static final String MDC_KEY = "requestId";

    private RequestCorrelation() {
    }

    /**
     * Returns the provided request ID if it is non-blank, or generates a new UUID string.
     *
     * @param requestId the value from the incoming {@code X-Request-Id} header; may be
     *                  {@code null} or blank
     * @return the existing ID if usable, or a freshly generated UUID string
     */
    public static String normalizeOrGenerate(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
