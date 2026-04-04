package com.opspilot.tenant.exception;

import java.time.Instant;

/** Uniform error response body returned by GlobalExceptionHandler for all error responses.
 * Fields: code (machine-readable), message (human-readable), timestamp, path. */
public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path
) {
}
