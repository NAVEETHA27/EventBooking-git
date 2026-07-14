package com.eventbooking.service;

import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.entity.ChatSession;
import com.eventbooking.repository.ChatSessionRepository;
import com.eventbooking.security.AuthPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationMemoryService {
    private static final TypeReference<List<AIChatRequest.ChatMessage>> MESSAGE_LIST = new TypeReference<>() {};

    private final ChatSessionRepository chatSessionRepository;
    private final ObjectMapper objectMapper;
    private final com.eventbooking.ai.AIEngine aiEngine;

    @Value("${eventgpt.max-history-messages:10}")
    private int maxHistoryMessages;

    @Transactional
    public ChatSession resolveSession(String sessionId, AuthPrincipal principal, String firstMessage) {
        if (StringUtils.hasText(sessionId)) {
            return chatSessionRepository.findBySessionId(sessionId)
                    .filter(session -> canAccess(session, principal))
                    .orElseGet(() -> createSession(principal, firstMessage));
        }
        return createSession(principal, firstMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatSession> recentSessions(AuthPrincipal principal) {
        if (principal == null) {
            return List.of();
        }
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(principal.getId(), PageRequest.of(0, 20));
    }

    public List<AIChatRequest.ChatMessage> history(ChatSession session) {
        try {
            List<AIChatRequest.ChatMessage> messages = objectMapper.readValue(session.getMessagesJson(), MESSAGE_LIST);
            List<AIChatRequest.ChatMessage> result = new ArrayList<>();
            if (session.getSummary() != null && !session.getSummary().isBlank()) {
                result.add(AIChatRequest.ChatMessage.builder()
                        .role("system")
                        .content("Context summary of earlier conversation: " + session.getSummary())
                        .build());
            }
            int from = Math.max(0, messages.size() - maxHistoryMessages);
            result.addAll(messages.subList(from, messages.size()));
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Transactional
    public void append(ChatSession session, String userMessage, String assistantMessage) {
        List<AIChatRequest.ChatMessage> messages;
        try {
            messages = new ArrayList<>(objectMapper.readValue(session.getMessagesJson(), MESSAGE_LIST));
        } catch (Exception ex) {
            messages = new ArrayList<>();
        }

        messages.add(AIChatRequest.ChatMessage.builder().role("user").content(userMessage).build());
        messages.add(AIChatRequest.ChatMessage.builder().role("assistant").content(assistantMessage).build());

        // Summarize and compress when history exceeds 12 messages
        if (messages.size() > 12 && aiEngine != null && aiEngine.isAvailable()) {
            try {
                List<AIChatRequest.ChatMessage> toSummarize = new ArrayList<>(messages.subList(0, 4));
                messages.subList(0, 4).clear();

                StringBuilder textToSummarize = new StringBuilder();
                if (session.getSummary() != null && !session.getSummary().isBlank()) {
                    textToSummarize.append("Previous Summary: ").append(session.getSummary()).append("\n");
                }
                for (AIChatRequest.ChatMessage m : toSummarize) {
                    textToSummarize.append(m.getRole()).append(": ").append(m.getContent()).append("\n");
                }

                String prompt = "Create a single-paragraph summary of the following conversation turns. Keep it under 60 words:\n" + textToSummarize.toString();
                String newSummary = aiEngine.complete("MEMORY_SUMMARY", "You are a conversation summarizer.", prompt);
                if (newSummary != null && !newSummary.isBlank()) {
                    session.setSummary(newSummary.trim());
                }
            } catch (Exception ex) {
                // Fallback: do not block message append on summarizer failure
            }
        }

        try {
            session.setMessagesJson(objectMapper.writeValueAsString(messages));
            chatSessionRepository.save(session);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to persist chat memory", ex);
        }
    }

    private ChatSession createSession(AuthPrincipal principal, String firstMessage) {
        String title = StringUtils.hasText(firstMessage) ? firstMessage.strip() : "New EventGPT chat";
        if (title.length() > 80) {
            title = title.substring(0, 80);
        }
        return chatSessionRepository.save(ChatSession.builder()
                .userId(principal != null ? principal.getId() : null)
                .userRole(principal != null ? principal.getRole() : "GUEST")
                .title(title)
                .messagesJson("[]")
                .build());
    }

    private boolean canAccess(ChatSession session, AuthPrincipal principal) {
        if (session.getUserId() == null) {
            return principal == null;
        }
        return principal != null && session.getUserId().equals(principal.getId());
    }
}
