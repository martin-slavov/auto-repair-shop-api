package com.example.auto_repair_shop_api.exception;

public class OwnershipViolationException extends RuntimeException {
    public OwnershipViolationException(String message) {
        super(message);
    }
}
