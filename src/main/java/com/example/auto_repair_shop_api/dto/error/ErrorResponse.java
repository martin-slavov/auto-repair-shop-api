package com.example.auto_repair_shop_api.dto.error;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors
) {
}