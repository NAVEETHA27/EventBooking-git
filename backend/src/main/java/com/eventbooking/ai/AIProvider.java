package com.eventbooking.ai;

import com.eventbooking.dto.request.AIChatRequest;

import java.util.List;

public interface AIProvider {
    String name();
    boolean isConfigured();
    String complete(String systemPrompt, String userMessage, List<AIChatRequest.ChatMessage> history);
}
