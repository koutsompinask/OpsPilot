package com.opspilot.assistant.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised exception handler for the assistant-service.
 *
 * Intercepts all exceptions thrown by controllers and maps them to structured
 * {@link ApiError} responses with appropriate HTTP status codes. The generic
 * {@link Exception} fallback returns a 500 with a non-revealing message to prevent
 * internal details from leaking to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Validation failed");
        log.warn("assistant_validation_error path={} message={}", request.getRequestURI(), message);
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request.getRequestURI());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        log.warn("assistant_bad_request path={} message={}", request.getRequestURI(), ex.getMessage());
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        log.warn("assistant_not_found path={} message={}", request.getRequestURI(), ex.getMessage());
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("assistant_forbidden path={} message={}", request.getRequestURI(), ex.getMessage());
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("assistant_unauthorized path={} message={}", request.getRequestURI(), ex.getMessage());
        return response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiError> handleStorage(StorageException ex, HttpServletRequest request) {
        log.error("assistant_storage_error path={} message={}", request.getRequestURI(), ex.getMessage(), ex);
        return response(HttpStatus.BAD_GATEWAY, "STORAGE_ERROR", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("assistant_internal_error path={} message={}", request.getRequestURI(), ex.getMessage(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", request.getRequestURI());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message, String path) {
        return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now(), path));
    }
}
