package com.eventbooking.ai.rag;

public record RagDocument(
        String sourceType,
        String sourceId,
        String title,
        String content,
        int score
) {
}
