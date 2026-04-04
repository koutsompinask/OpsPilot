package com.opspilot.ticket.exception;

import java.time.Instant;

/**
 * Uniform error response body returned by {@link GlobalExceptionHandler} for all error responses.
 *
 * @param timestamp the time at which the error occurred
 * @param status    the HTTP status code
 * @param error     the HTTP status reason phrase
 * @param code      a machine-readable error code (e.g. {@code "NOT_FOUND"}, {@code "FORBIDDEN"})
 * @param message   a human-readable description of the error
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message
) {
}
