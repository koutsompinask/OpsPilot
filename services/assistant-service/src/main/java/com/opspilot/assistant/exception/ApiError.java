package com.opspilot.assistant.exception;

import java.time.Instant;

/**
 * Uniform error response body returned by {@link GlobalExceptionHandler} for all error responses.
 *
 * @param code      a machine-readable error code (e.g. {@code "NOT_FOUND"}, {@code "VALIDATION_ERROR"})
 * @param message   a human-readable description of the error
 * @param timestamp the time at which the error occurred
 * @param path      the request path that triggered the error
 */
public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path
) {
}
