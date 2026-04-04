package com.opspilot.assistant.exception;

/**
 * Thrown when a downstream AI provider (LLM or embedding service) is unavailable or returns
 * an unexpected error. Maps to HTTP 503 Service Unavailable so clients can distinguish a
 * transient infrastructure failure from an application-level error (500).
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
