package com.company.api.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Validation error response for API validation failures
 *
 * Extends standard error response to include field-level validation errors.
 * Used when request validation fails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {

    /**
     * Error code
     */
    private String code;

    /**
     * Error message
     */
    private String message;

    /**
     * Timestamp when the error occurred
     */
    private Long timestamp;

    /**
     * Map of field names to validation error messages
     */
    private Map<String, String> errors;
}
