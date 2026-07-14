package com.eventbooking.ai.rag;

import java.util.List;

public interface VectorStoreProvider {
    String providerName();
    int index(String sourceType, String sourceId, String title, List<String> chunks);
    List<RagDocument> search(String query, int topK);
    long count();
}
