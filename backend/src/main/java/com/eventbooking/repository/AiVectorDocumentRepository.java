package com.eventbooking.repository;

import com.eventbooking.entity.AiVectorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiVectorDocumentRepository extends JpaRepository<AiVectorDocument, Long> {
    Optional<AiVectorDocument> findBySourceTypeAndSourceIdAndChunkIndex(String sourceType, String sourceId, int chunkIndex);
    List<AiVectorDocument> findBySourceTypeAndSourceId(String sourceType, String sourceId);

    // Optimized projection query
    List<VectorProjection> findAllProjectionsBy();

    interface VectorProjection {
        Long getId();
        String getEmbeddingJson();
    }
}
