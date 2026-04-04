package com.opspilot.tenant.exception;

/** Thrown when a request is missing or carries an invalid authentication credential. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
