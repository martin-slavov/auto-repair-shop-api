package com.example.auto_repair_shop_api.service.chat;

public record ChatPromptMessage(String role, String content) {

    public static ChatPromptMessage system(String content) {
        return new ChatPromptMessage("system", content);
    }

    public static ChatPromptMessage user(String content) {
        return new ChatPromptMessage("user", content);
    }

    public static ChatPromptMessage assistant(String content) {
        return new ChatPromptMessage("assistant", content);
    }
}
