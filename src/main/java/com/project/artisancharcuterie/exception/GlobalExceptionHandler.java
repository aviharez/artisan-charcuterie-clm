package com.project.artisancharcuterie.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized error handling that returns a consistent JSON error envelope
 * for all HTTP error conditions (400, 404, 422, 500).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 404 Not Found

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 422 Unprocessable Entity

    @ExceptionHandler(IllegalTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalTransition(
            IllegalTransitionException ex, WebRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(ImmutableResourceException.class)
    public ResponseEntity<Map<String, Object>> handleImmutable(
            ImmutableResourceException ex, WebRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    // 400 Bad Request

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, fieldErrors, request);

        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
        body.put("fieldErrors", fields);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = String.format("parameter '%s' has an invalid value: '%s'", ex.getName(), ex.getValue());
        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 500 Internal Server Error

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred. Please contact support. ", request);
    }

    // helpers

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status).body(buildBody(status, message, request));
    }

    private Map<String, Object> buildBody(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return body;
    }

}
