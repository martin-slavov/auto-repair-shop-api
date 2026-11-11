package com.example.auto_repair_shop_api.exception;

/**
 * Thrown when the "sort" query parameter names a field that is not whitelisted
 * or uses a direction other than asc/desc. Mapped to 400 by the global handler.
 */
public class InvalidSortParameterException extends RuntimeException {
    public InvalidSortParameterException(String message) {
        super(message);
    }
}
