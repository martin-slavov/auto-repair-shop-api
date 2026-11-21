package com.example.auto_repair_shop_api.exception;

public class ChatUnavailableException extends RuntimeException {
    public ChatUnavailableException() {
        super("The assistant is temporarily unavailable. Please try again later.");
    }
}
