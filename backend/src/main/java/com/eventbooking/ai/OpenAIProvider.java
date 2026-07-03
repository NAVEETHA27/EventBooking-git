package com.eventbooking.ai;

import com.eventbooking.dto.request.AIChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OpenAIProvider implements AIProvider {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final String apiKey;
    private final String model;
    private final String endpoint;

    public OpenAIProvider(ObjectMapper objectMapper,
                          @Value("${ai.openai.api-key:}") String apiKey,
                          @Value("${ai.openai.model:gpt-4o-mini}") String model,
                          @Value("${ai.openai.endpoint:https://api.openai.com/v1/chat/completions}") String endpoint) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
    }

    @Override public String name() { return "openai"; }
    @Override public boolean isConfigured() { return StringUtils.hasText(apiKey); }

    @Override
    public String complete(String systemPrompt, String userMessage, List<AIChatRequest.ChatMessage> history) {
        if (!isConfigured()) throw new IllegalStateException("OpenAI API key is not configured");
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            history.stream().limit(8).forEach(m -> {
                String role = "assistant".equalsIgnoreCase(m.getRole()) ? "assistant" : "user";
                messages.add(Map.of("role", role, "content", safe(m.getContent())));
            });
            messages.add(Map.of("role", "user", "content", userMessage));
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.3,
                    "messages", messages
            ));
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI request failed with status {}", response.statusCode());
                throw new IllegalStateException("OpenAI request failed");
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception ex) {
            throw new IllegalStateException("OpenAI provider failed", ex);
        }
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.length() > 1200 ? value.substring(0, 1200) : value;
    }
}
