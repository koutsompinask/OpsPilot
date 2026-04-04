package com.opspilot.auth.exception;

/** Thrown when the authenticated user does not have permission to perform the requested operation. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
