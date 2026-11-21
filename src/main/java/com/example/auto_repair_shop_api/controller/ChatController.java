package com.example.auto_repair_shop_api.controller;

import com.example.auto_repair_shop_api.dto.chat.ChatRequestDTO;
import com.example.auto_repair_shop_api.dto.chat.ChatResponseDTO;
import com.example.auto_repair_shop_api.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "Shop assistant chatbot")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "Ask the shop assistant",
            description = "Access: public. Answers questions about services, approximate prices and booking, "
                    + "based on the active catalog and the booking rules. Rate limited per client IP and per day.")
    @ApiResponse(responseCode = "200", description = "Assistant reply",
            content = @Content(schema = @Schema(implementation = ChatResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "429", description = "Too many requests (per IP or daily limit)")
    @ApiResponse(responseCode = "503", description = "The assistant is temporarily unavailable")
    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request,
                                                HttpServletRequest httpRequest) {
        return ResponseEntity.ok(chatService.chat(request, httpRequest.getRemoteAddr()));
    }
}
