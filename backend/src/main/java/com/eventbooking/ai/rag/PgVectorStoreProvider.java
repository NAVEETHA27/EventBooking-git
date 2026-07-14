package com.eventbooking.ai.rag;

import com.eventbooking.config.AIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PgVectorStoreProvider implements VectorStoreProvider {
    private final AIConfig aiConfig;
    private final EmbeddingService embeddingService;
    private final MySqlVectorStoreProvider fallbackProvider;

    @Override
    public String providerName() {
        return "pgvector";
    }

    @Override
    public int index(String sourceType, String sourceId, String title, List<String> chunks) {
        if (!configured()) {
            return fallbackProvider.index(sourceType, sourceId, title, chunks);
        }
        try (Connection connection = openConnection()) {
            int updated = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String content = chunks.get(i);
                String hash = DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
                if (upsert(connection, sourceType, sourceId, i, limit(title, 220), content, hash,
                        embeddingService.embed(content)) > 0) {
                    updated++;
                }
            }
            return updated;
        } catch (Exception ex) {
            log.warn("PGVector index failed, falling back to MySQL vector store: {}", ex.getMessage());
            return fallbackProvider.index(sourceType, sourceId, title, chunks);
        }
    }

    @Override
    public List<RagDocument> search(String query, int topK) {
        if (!configured()) {
            return fallbackProvider.search(query, topK);
        }
        String sql = """
                SELECT source_type, source_id, title, content, 1 - (embedding <=> CAST(? AS vector)) AS score
                FROM eventgpt_vector_documents
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;
        String vector = vectorLiteral(embeddingService.embed(query));
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vector);
            statement.setString(2, vector);
            statement.setInt(3, topK);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<RagDocument> documents = new ArrayList<>();
                while (resultSet.next()) {
                    double score = resultSet.getDouble("score");
                    if (score <= 0.05d) {
                        continue;
                    }
                    documents.add(new RagDocument(
                            resultSet.getString("source_type"),
                            resultSet.getString("source_id"),
                            resultSet.getString("title"),
                            resultSet.getString("content"),
                            (int) Math.round(score * 100)));
                }
                return documents;
            }
        } catch (Exception ex) {
            log.warn("PGVector search failed, falling back to MySQL vector store: {}", ex.getMessage());
            return fallbackProvider.search(query, topK);
        }
    }

    @Override
    public long count() {
        if (!configured()) {
            return fallbackProvider.count();
        }
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM eventgpt_vector_documents");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (Exception ex) {
            log.warn("PGVector count failed, falling back to MySQL vector store: {}", ex.getMessage());
            return fallbackProvider.count();
        }
    }

    public boolean configured() {
        return aiConfig.getVectorDb() != null
                && "pgvector".equalsIgnoreCase(aiConfig.getVectorDb().getProvider())
                && aiConfig.getVectorDb().getUrl() != null
                && !aiConfig.getVectorDb().getUrl().isBlank();
    }

    private int upsert(Connection connection, String sourceType, String sourceId, int chunkIndex,
                       String title, String content, String contentHash, float[] embedding) throws Exception {
        String sql = """
                INSERT INTO eventgpt_vector_documents
                    (source_type, source_id, chunk_index, title, content, content_hash, embedding, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS vector), ?, ?)
                ON CONFLICT (source_type, source_id, chunk_index)
                DO UPDATE SET
                    title = EXCLUDED.title,
                    content = EXCLUDED.content,
                    content_hash = EXCLUDED.content_hash,
                    embedding = EXCLUDED.embedding,
                    updated_at = EXCLUDED.updated_at
                WHERE eventgpt_vector_documents.content_hash <> EXCLUDED.content_hash
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            statement.setString(1, sourceType);
            statement.setString(2, sourceId);
            statement.setInt(3, chunkIndex);
            statement.setString(4, title);
            statement.setString(5, content);
            statement.setString(6, contentHash);
            statement.setString(7, vectorLiteral(embedding));
            statement.setTimestamp(8, now);
            statement.setTimestamp(9, now);
            return statement.executeUpdate();
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                aiConfig.getVectorDb().getUrl(),
                aiConfig.getVectorDb().getUsername(),
                aiConfig.getVectorDb().getPassword());
    }

    private String vectorLiteral(float[] embedding) {
        StringBuilder literal = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(embedding[i]);
        }
        return literal.append(']').toString();
    }

    private String limit(String value, int max) {
        if (value == null) {
            return "Untitled";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
