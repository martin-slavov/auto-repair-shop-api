package com.example.auto_repair_shop_api.dto.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequestDTO(
        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must be at most 500 characters")
        String message,

        @Size(max = 6, message = "History can contain at most 6 messages")
        List<@Valid ChatMessageDTO> history
) {
}
