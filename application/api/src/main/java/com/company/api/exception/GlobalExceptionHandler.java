package com.company.api.exception;

import com.company.common.exception.ApplicationException;
import com.company.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST endpoints
 *
 * This class centralizes exception handling across all REST controllers.
 * It converts application exceptions into appropriate HTTP responses.
 *
 * Handled exceptions:
 * - ApplicationException: 400 Bad Request
 * - ResourceNotFoundException: 404 Not Found
 * - MethodArgumentNotValidException: 400 Bad Request (validation errors)
 * - Generic Exception: 500 Internal Server Error
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle application-specific exceptions
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
        ApplicationException ex) {
        log.error("Application exception: {} - {}", ex.getCode(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code(ex.getCode())
            .message(ex.getMessage())
            .timestamp(System.currentTimeMillis())
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
        ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code(ex.getCode())
            .message(ex.getMessage())
            .timestamp(System.currentTimeMillis())
            .build();

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse);
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex) {
        log.error("Validation error occurred");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
            .code("VALIDATION_FAILED")
            .message("Validation failed")
            .timestamp(System.currentTimeMillis())
            .errors(fieldErrors)
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .timestamp(System.currentTimeMillis())
            .build();

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }
}
