package com.eventbooking.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AIChatResponse {
    private String sessionId;
    private String answer;
    private String provider;
    private LocalDateTime timestamp;
    @Builder.Default
    private List<QuickAction> actions = new ArrayList<>();
    @Builder.Default
    private List<Source> sources = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuickAction {
        private String label;
        private String path;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Source {
        private String type;
        private String id;
        private String title;
    }
}
