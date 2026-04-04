package com.opspilot.ticket.exception;

/** Thrown when a requested ticket does not exist within the current tenant's scope. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
