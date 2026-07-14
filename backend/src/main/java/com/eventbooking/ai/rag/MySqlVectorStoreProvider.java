package com.eventbooking.ai.rag;

import com.eventbooking.entity.AiVectorDocument;
import com.eventbooking.repository.AiVectorDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MySqlVectorStoreProvider implements VectorStoreProvider {
    private final AiVectorDocumentRepository vectorDocumentRepository;
    private final EmbeddingService embeddingService;

    @Override
    public String providerName() {
        return "mysql";
    }

    @Override
    @Transactional
    public int index(String sourceType, String sourceId, String title, List<String> chunks) {
        List<AiVectorDocument> pending = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            String hash = DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
            AiVectorDocument document = vectorDocumentRepository
                    .findBySourceTypeAndSourceIdAndChunkIndex(sourceType, sourceId, i)
                    .orElseGet(AiVectorDocument::new);
            if (hash.equals(document.getContentHash())) {
                continue;
            }
            document.setSourceType(sourceType);
            document.setSourceId(sourceId);
            document.setChunkIndex(i);
            document.setTitle(limit(title, 220));
            document.setContent(content);
            document.setContentHash(hash);
            document.setEmbeddingJson(embeddingService.toJson(embeddingService.embed(content)));
            pending.add(document);
        }
        vectorDocumentRepository.saveAll(pending);
        return pending.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RagDocument> search(String query, int topK) {
        float[] queryEmbedding = embeddingService.embed(query);
        List<AiVectorDocumentRepository.VectorProjection> projections = vectorDocumentRepository.findAllProjectionsBy();
        if (projections == null || projections.isEmpty()) {
            return List.of();
        }

        List<ScoredProjection> matched = projections.stream()
                .map(p -> new ScoredProjection(p.getId(),
                        embeddingService.cosine(queryEmbedding, embeddingService.fromJson(p.getEmbeddingJson()))))
                .filter(scored -> scored.score() > 0.05d)
                .sorted(Comparator.comparingDouble(ScoredProjection::score).reversed())
                .limit(topK)
                .toList();

        if (matched.isEmpty()) {
            return List.of();
        }

        List<Long> matchedIds = matched.stream().map(ScoredProjection::id).toList();
        List<AiVectorDocument> matchedDocs = vectorDocumentRepository.findAllById(matchedIds);
        Map<Long, AiVectorDocument> docMap = matchedDocs.stream()
                .collect(Collectors.toMap(AiVectorDocument::getId, doc -> doc));

        return matched.stream()
                .map(m -> docMap.get(m.id()))
                .filter(Objects::nonNull)
                .map(doc -> {
                    double score = embeddingService.cosine(queryEmbedding, embeddingService.fromJson(doc.getEmbeddingJson()));
                    return new RagDocument(
                            doc.getSourceType(),
                            doc.getSourceId(),
                            doc.getTitle(),
                            doc.getContent(),
                            (int) Math.round(score * 100));
                })
                .toList();
    }

    @Override
    public long count() {
        return vectorDocumentRepository.count();
    }

    private String limit(String value, int max) {
        if (value == null) {
            return "Untitled";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record ScoredProjection(Long id, double score) {
    }
}
