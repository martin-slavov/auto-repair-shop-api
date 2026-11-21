package com.example.auto_repair_shop_api.service.chat;

import java.util.List;

public interface CloudflareAiClient {
    String complete(List<ChatPromptMessage> messages);
}
