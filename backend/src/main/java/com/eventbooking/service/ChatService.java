package com.eventbooking.service;

import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.dto.response.AIChatResponse;
import com.eventbooking.entity.ChatSession;
import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final AIService aiService;
    private final ConversationMemoryService conversationMemoryService;

    @Transactional
    public AIChatResponse chat(AIChatRequest request, AuthPrincipal principal, String remoteAddress) {
        ChatSession session = conversationMemoryService.resolveSession(
                request != null ? request.getSessionId() : null,
                principal,
                request != null ? request.getMessage() : null);

        AIChatRequest enriched = AIChatRequest.builder()
                .message(request != null ? request.getMessage() : null)
                .sessionId(session.getSessionId())
                .history(conversationMemoryService.history(session))
                .build();

        AIChatResponse response = aiService.chat(enriched, principal, remoteAddress);
        response.setSessionId(session.getSessionId());
        conversationMemoryService.append(session, enriched.getMessage(), response.getAnswer());
        return response;
    }
}
