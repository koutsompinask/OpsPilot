package com.opspilot.ticket.util.logging;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Utility class for propagating and accessing the per-request correlation ID.
 *
 * <p>The correlation ID is set once per request by {@link CorrelationIdFilter} (reading the
 * incoming {@code X-Request-Id} header or generating a fresh UUID). It is stored in the
 * SLF4J MDC under the key {@link #MDC_KEY} so that every log line emitted during the request
 * automatically includes the correlation ID. It is also stamped on newly created tickets so
 * that tickets can be traced back to the request that triggered their creation.</p>
 *
 * <p>This class is a non-instantiable static utility.</p>
 */
public final class RequestCorrelation {

    /** HTTP header name used to carry the correlation ID between services. */
    public static final String HEADER_NAME = "X-Request-Id";

    /** SLF4J MDC key under which the correlation ID is stored for the duration of a request. */
    public static final String MDC_KEY = "requestId";

    private RequestCorrelation() {
    }

    /**
     * Returns a sanitised correlation ID, generating a fresh UUID if the input is absent.
     *
     * <p>The value is trimmed and capped at 128 characters to satisfy the database column
     * constraint on {@code tickets.created_request_id}.</p>
     *
     * @param value the raw correlation ID string from an HTTP header or MDC; may be {@code null}
     * @return a non-blank correlation ID, at most 128 characters long
     */
    public static String normalizeOrGenerate(String value) {
        if (value == null || value.isBlank()) {
            // No ID was provided — generate a fresh one so every request is traceable
            return UUID.randomUUID().toString();
        }
        String trimmed = value.trim();
        // Cap at 128 chars to match the tickets.created_request_id column length
        return trimmed.length() > 128 ? trimmed.substring(0, 128) : trimmed;
    }

    /**
     * Returns the correlation ID currently stored in the MDC for the active request thread.
     *
     * <p>Falls back to a generated UUID if the MDC entry is absent, which can happen on
     * threads that did not pass through {@link CorrelationIdFilter} (e.g. async tasks).</p>
     *
     * @return the current request's correlation ID, never {@code null}
     */
    public static String currentRequestId() {
        return normalizeOrGenerate(MDC.get(MDC_KEY));
    }
}
