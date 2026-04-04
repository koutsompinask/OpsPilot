package com.opspilot.notification.util.logging;

import java.util.UUID;

/**
 * Utility for managing the per-request correlation ID stored in the SLF4J MDC.
 *
 * <p>The correlation ID is propagated from upstream services via the event's {@code requestId}
 * field. When the ID is absent or blank — for example when an event was published by a service
 * that does not yet carry the field — a random UUID is generated so that all log lines emitted
 * during processing of that event are still grouped under a consistent ID.</p>
 *
 * <p>This class is a non-instantiable utility holder; all members are static.</p>
 */
public final class RequestCorrelation {

    /** MDC key under which the correlation ID is stored, matching the logback pattern {@code %X{requestId}}. */
    public static final String MDC_KEY = "requestId";

    private RequestCorrelation() {
    }

    /**
     * Returns a sanitised correlation ID ready to be placed in the MDC.
     *
     * <p>If {@code value} is null or blank a fresh random UUID is returned so there is always a
     * non-empty value in the MDC. Values longer than 128 characters are truncated to prevent
     * excessively long strings appearing in log output (e.g. from malformed upstream events).</p>
     *
     * @param value the raw correlation ID from the incoming event, may be null or blank
     * @return a non-null, non-blank correlation ID no longer than 128 characters
     */
    public static String normalizeOrGenerate(String value) {
        if (value == null || value.isBlank()) {
            // No ID supplied by the publisher — generate a local one so logs remain traceable
            return UUID.randomUUID().toString();
        }
        String trimmed = value.trim();
        // Cap length at 128 characters to guard against pathologically long IDs in log output
        return trimmed.length() > 128 ? trimmed.substring(0, 128) : trimmed;
    }
}
