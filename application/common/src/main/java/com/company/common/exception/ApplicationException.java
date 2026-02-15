package com.company.common.exception;

/**
 * Base exception class for the entire application
 *
 * All application-specific exceptions should extend this class
 * or one of its subclasses to provide consistent error handling
 */
public class ApplicationException extends RuntimeException {

    /**
     * Exception code for identifying error types
     */
    private final String code;

    /**
     * Create a new ApplicationException with code and message
     */
    public ApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Create a new ApplicationException with code, message, and cause
     */
    public ApplicationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
