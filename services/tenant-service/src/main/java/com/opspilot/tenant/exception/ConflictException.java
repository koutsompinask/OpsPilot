package com.opspilot.tenant.exception;

/** Thrown when a resource already exists and the operation would create a duplicate (e.g. duplicate email on user creation). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
