package com.example.auto_repair_shop_api.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessageDTO(
        @NotNull(message = "Role is required")
        ChatRole role,

        @NotBlank(message = "Content is required")
        @Size(max = 1000, message = "Content must be at most 1000 characters")
        String content
) {
}
