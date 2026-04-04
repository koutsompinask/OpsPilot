package com.opspilot.tenant.exception;

/** Thrown when a requested resource does not exist within the current tenant's scope. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
