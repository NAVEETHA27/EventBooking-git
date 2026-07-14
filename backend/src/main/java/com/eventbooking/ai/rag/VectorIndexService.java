package com.eventbooking.ai.rag;

import com.eventbooking.config.AIConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorIndexService {
    private final AIConfig aiConfig;
    private final MySqlVectorStoreProvider mySqlVectorStoreProvider;
    private final PgVectorStoreProvider pgVectorStoreProvider;

    public int index(String sourceType, String sourceId, String title, List<String> chunks) {
        return provider().index(sourceType, sourceId, title, chunks);
    }

    public List<RagDocument> search(String query, int topK) {
        return provider().search(query, topK);
    }

    public String providerName() {
        return provider().providerName();
    }

    public long count() {
        return provider().count();
    }

    private VectorStoreProvider provider() {
        String configuredProvider = aiConfig.getVectorDb() != null ? aiConfig.getVectorDb().getProvider() : "mysql";
        if ("pgvector".equalsIgnoreCase(configuredProvider)) {
            return pgVectorStoreProvider;
        }
        return mySqlVectorStoreProvider;
    }
}
