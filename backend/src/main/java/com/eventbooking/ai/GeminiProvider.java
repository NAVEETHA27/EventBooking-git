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
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiProvider implements AIProvider {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final String apiKey;
    private final String model;

    public GeminiProvider(ObjectMapper objectMapper,
                          @Value("${ai.gemini.api-key:}") String apiKey,
                          @Value("${ai.gemini.model:gemini-1.5-flash}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override public String name() { return "gemini"; }
    @Override public boolean isConfigured() { return StringUtils.hasText(apiKey); }

    @Override
    public String complete(String systemPrompt, String userMessage, List<AIChatRequest.ChatMessage> history) {
        if (!isConfigured()) throw new IllegalStateException("Gemini API key is not configured");
        try {
            StringBuilder prompt = new StringBuilder(systemPrompt).append("\n\nConversation:\n");
            history.stream().limit(8).forEach(m -> prompt.append(m.getRole()).append(": ").append(safe(m.getContent())).append("\n"));
            prompt.append("user: ").append(userMessage);
            String body = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt.toString()))))
            ));
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini request failed with status {}", response.statusCode());
                throw new IllegalStateException("Gemini request failed");
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
        } catch (Exception ex) {
            throw new IllegalStateException("Gemini provider failed", ex);
        }
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.length() > 1200 ? value.substring(0, 1200) : value;
    }
}
