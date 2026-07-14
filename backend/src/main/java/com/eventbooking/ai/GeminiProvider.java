package com.eventbooking.ai;

import com.eventbooking.dto.request.AIChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI implementation for Google Gemini Provider.
 */
@Component
@Slf4j
public class GeminiProvider implements AIProvider {

    private final GoogleGenAiChatModel chatModel;

    public GeminiProvider(@Autowired(required = false) GoogleGenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public boolean isConfigured() {
        return chatModel != null;
    }

    @Override
    public String complete(String systemPrompt, String userMessage, List<AIChatRequest.ChatMessage> history) {
        if (!isConfigured()) {
            throw new IllegalStateException("Google Gemini ChatModel is not configured.");
        }

        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        if (history != null) {
            history.stream().limit(10).forEach(turn -> {
                if ("assistant".equalsIgnoreCase(turn.getRole())) {
                    messages.add(new AssistantMessage(turn.getContent()));
                } else {
                    messages.add(new UserMessage(turn.getContent()));
                }
            });
        }

        messages.add(new UserMessage(userMessage));

        log.debug("[GeminiProvider] Invoking Google GenAI ChatModel...");
        try {
            ChatResponse response = chatModel.call(new Prompt(messages));
            if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText().trim();
            }
            return "";
        } catch (Exception ex) {
            log.error("[GeminiProvider] Chat completion error: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Gemini completion failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public org.reactivestreams.Publisher<String> streamComplete(String systemPrompt, String userMessage, List<AIChatRequest.ChatMessage> history) {
        if (!isConfigured()) {
            throw new IllegalStateException("Google Gemini ChatModel is not configured.");
        }

        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        if (history != null) {
            history.stream().limit(10).forEach(turn -> {
                if ("assistant".equalsIgnoreCase(turn.getRole())) {
                    messages.add(new AssistantMessage(turn.getContent()));
                } else {
                    messages.add(new UserMessage(turn.getContent()));
                }
            });
        }

        messages.add(new UserMessage(userMessage));

        log.debug("[GeminiProvider] Invoking Google GenAI StreamingChatModel...");
        try {
            return chatModel.stream(new Prompt(messages))
                    .map(response -> {
                        if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                            String c = response.getResult().getOutput().getText();
                            return c != null ? c : "";
                        }
                        return "";
                    });
        } catch (Exception ex) {
            log.error("[GeminiProvider] Chat streaming error: {}", ex.getMessage(), ex);
            return reactor.core.publisher.Flux.error(ex);
        }
    }
}
