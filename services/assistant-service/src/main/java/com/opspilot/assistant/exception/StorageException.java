package com.opspilot.assistant.exception;

/** Thrown when an interaction with the MinIO object storage backend fails. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
