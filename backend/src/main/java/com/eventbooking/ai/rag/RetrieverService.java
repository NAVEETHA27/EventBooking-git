package com.eventbooking.ai.rag;

import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RetrieverService {
    private final EventKnowledgeRetrievalService knowledgeRetrievalService;
    private final VectorIndexService vectorIndexService;

    @Value("${eventgpt.max-context-documents:12}")
    private int maxContextDocuments;

    @Transactional(readOnly = true)
    public List<RagDocument> retrieve(String query, AuthPrincipal principal) {
        Map<String, RagDocument> merged = new LinkedHashMap<>();
        List<RagDocument> vectorDocuments = vectorIndexService.search(query, maxContextDocuments);
        List<RagDocument> scopedDocuments = knowledgeRetrievalService.retrieve(query, principal);
        List<RagDocument> all = new ArrayList<>();
        all.addAll(vectorDocuments);
        all.addAll(scopedDocuments);
        all.stream()
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .forEach(document -> merged.putIfAbsent(document.sourceType() + ":" + document.sourceId() + ":" + document.title(), document));
        return merged.values().stream().limit(maxContextDocuments).toList();
    }
}
