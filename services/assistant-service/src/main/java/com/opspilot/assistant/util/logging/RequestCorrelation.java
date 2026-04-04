package com.opspilot.assistant.util.logging;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Utility class for managing the request correlation ID used across all service calls.
 *
 * The correlation ID is stored in MDC under the key {@link #MDC_KEY} and propagated between
 * services via the {@link #HEADER_NAME} HTTP header. It is generated at the API gateway and
 * forwarded downstream; each service reads it from the header and writes it into MDC via
 * {@link CorrelationIdFilter}.
 *
 * This class is non-instantiable — all members are static.
 */
public final class RequestCorrelation {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private RequestCorrelation() {
    }

    /**
     * Returns the provided correlation ID value if valid, or generates a new UUID if null/blank.
     * Values longer than 128 characters are truncated to prevent log injection.
     *
     * @param value an incoming {@code X-Request-Id} header value, may be null
     * @return a non-null, non-blank correlation ID
     */
    public static String normalizeOrGenerate(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String trimmed = value.trim();
        return trimmed.length() > 128 ? trimmed.substring(0, 128) : trimmed;
    }

    /**
     * Returns the current request's correlation ID from MDC, or generates a new one if none is set.
     *
     * @return the active correlation ID for the current thread
     */
    public static String currentRequestId() {
        return normalizeOrGenerate(MDC.get(MDC_KEY));
    }
}
