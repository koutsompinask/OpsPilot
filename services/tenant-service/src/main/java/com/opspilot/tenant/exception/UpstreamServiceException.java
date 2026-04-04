package com.opspilot.tenant.exception;

/** Thrown when a synchronous call to a downstream service (e.g. auth-service) returns an error response. */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message) {
        super(message);
    }
}
