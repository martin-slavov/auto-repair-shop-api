package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.chat.ChatRequestDTO;
import com.example.auto_repair_shop_api.dto.chat.ChatResponseDTO;

public interface ChatService {
    ChatResponseDTO chat(ChatRequestDTO request, String clientIp);
}
