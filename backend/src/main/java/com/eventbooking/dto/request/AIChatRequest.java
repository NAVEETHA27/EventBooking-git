package com.eventbooking.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AIChatRequest {
    @Size(max = 1200, message = "Message is too long")
    private String message;

    private String sessionId;

    @Builder.Default
    private List<ChatMessage> history = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChatMessage {
        private String role;
        @Size(max = 1200)
        private String content;
    }
}
