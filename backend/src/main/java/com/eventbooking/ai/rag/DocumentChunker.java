package com.eventbooking.ai.rag;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {
    private static final int MAX_CHARS = 900;
    private static final int OVERLAP_CHARS = 120;

    public List<String> chunk(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_CHARS) {
            return List.of(normalized);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + MAX_CHARS);
            int sentenceEnd = normalized.lastIndexOf(". ", end);
            if (sentenceEnd > start + 250) {
                end = sentenceEnd + 1;
            }
            chunks.add(normalized.substring(start, end).trim());
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(0, end - OVERLAP_CHARS);
        }
        return chunks;
    }
}
