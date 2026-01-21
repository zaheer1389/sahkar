package com.badargadh.sahkar.exception;

/**
 * Custom exception to handle business logic violations.
 * Extending RuntimeException ensures automatic transaction rollback in Spring.
 */
public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}