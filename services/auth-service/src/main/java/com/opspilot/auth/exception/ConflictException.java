package com.opspilot.auth.exception;

/** Thrown when a resource already exists and the operation would create a duplicate (e.g. duplicate email on registration). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
