package com.company.api.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error response structure for REST API
 *
 * All error responses from the API follow this format for consistency.
 * This allows clients to handle errors in a predictable way.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Error code for identifying the specific error type
     */
    private String code;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Timestamp when the error occurred
     */
    private Long timestamp;
}
