package com.eventbooking.controller;

import com.eventbooking.ai.rag.DocumentIndexer;
import com.eventbooking.ai.rag.VectorIndexService;
import com.eventbooking.config.AIConfig;
import com.eventbooking.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai/rag")
@RequiredArgsConstructor
public class RagController {
    private final DocumentIndexer documentIndexer;
    private final VectorIndexService vectorIndexService;
    private final AIConfig aiConfig;

    @PostMapping("/index")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> index() {
        int indexed = documentIndexer.indexCoreKnowledge();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "indexedChunks", indexed,
                "status", "completed")));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        String provider = aiConfig.getVectorDb() != null ? aiConfig.getVectorDb().getProvider() : "mysql";
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "provider", vectorIndexService.providerName(),
                "configuredProvider", provider,
                "mode", "pgvector".equalsIgnoreCase(provider) ? "PGVECTOR_DIRECT_WITH_FALLBACK" : "MYSQL_FALLBACK",
                "indexedDocuments", vectorIndexService.count(),
                "embeddingCache", "enabled",
                "batchIndexing", "enabled")));
    }
}
