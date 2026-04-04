package com.opspilot.auth.exception;

/** Thrown when a synchronous call to a downstream service (e.g. tenant-service) returns an error response. */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message) {
        super(message);
    }
}
