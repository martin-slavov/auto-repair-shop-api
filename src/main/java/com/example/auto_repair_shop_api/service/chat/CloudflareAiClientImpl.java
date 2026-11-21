package com.example.auto_repair_shop_api.service.chat;

import com.example.auto_repair_shop_api.config.ChatProperties;
import com.example.auto_repair_shop_api.exception.ChatUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CloudflareAiClientImpl implements CloudflareAiClient {

    private static final Logger log = LoggerFactory.getLogger(CloudflareAiClientImpl.class);
    private static final String API_BASE = "https://api.cloudflare.com/client/v4/accounts/";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;
    private final ChatProperties properties;

    public CloudflareAiClientImpl(RestClient cloudflareRestClient, ChatProperties properties) {
        this.restClient = cloudflareRestClient;
        this.properties = properties;
    }

    @Override
    public String complete(List<ChatPromptMessage> messages) {
        ChatProperties.Cloudflare cloudflare = properties.getCloudflare();
        if (!cloudflare.isConfigured()) {
            log.warn("Cloudflare Workers AI is not configured");
            throw new ChatUnavailableException();
        }
        String model = cloudflare.getModel().trim();

        String body;
        try {
            body = restClient.post()
                    .uri(URI.create(API_BASE + cloudflare.getAccountId().trim() + "/ai/run/" + model))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cloudflare.getApiToken().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(JSON.writeValueAsString(requestBody(messages)))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            log.warn("Cloudflare Workers AI returned status {}", ex.getStatusCode().value());
            throw new ChatUnavailableException();
        } catch (RuntimeException ex) {
            log.warn("Cloudflare Workers AI request failed: {}", ex.getClass().getSimpleName());
            throw new ChatUnavailableException();
        }

        String reply = extractReply(body);
        if (reply.isEmpty()) {
            log.warn("Cloudflare Workers AI returned no answer text");
            throw new ChatUnavailableException();
        }
        return reply;
    }

    private Map<String, Object> requestBody(List<ChatPromptMessage> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", messages.stream()
                .map(message -> Map.of("role", message.role(), "content", message.content()))
                .toList());
        body.put("max_tokens", properties.getMaxTokens());
        body.put("temperature", properties.getTemperature());
        return body;
    }

    static String extractReply(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (RuntimeException ex) {
            return "";
        }
        if (root.path("success").isBoolean() && !root.path("success").asBoolean()) {
            return "";
        }
        JsonNode result = root.has("result") ? root.path("result") : root;
        JsonNode response = result.path("response");
        return response.isString() ? response.asString().trim() : "";
    }
}
