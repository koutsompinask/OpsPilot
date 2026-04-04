package com.opspilot.assistant.exception;

/** Thrown when the request contains invalid or semantically incorrect input. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
