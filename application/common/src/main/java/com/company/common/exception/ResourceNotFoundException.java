package com.company.common.exception;

/**
 * Exception thrown when a requested resource is not found
 *
 * HTTP Status: 404 Not Found
 */
public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super(
            "RESOURCE_NOT_FOUND",
            String.format("%s with ID %d not found", resourceType, id)
        );
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
