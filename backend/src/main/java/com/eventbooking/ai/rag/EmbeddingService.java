package com.eventbooking.ai.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
    private static final int DIMENSIONS = 96;

    private final ObjectMapper objectMapper;
    private final Map<String, float[]> cache = new ConcurrentHashMap<>();

    public float[] embed(String text) {
        String normalized = StringUtils.hasText(text) ? text.toLowerCase().replaceAll("\\s+", " ").trim() : "";
        String hash = DigestUtils.md5DigestAsHex(normalized.getBytes(StandardCharsets.UTF_8));
        return cache.computeIfAbsent(hash, ignored -> localEmbedding(normalized));
    }

    public String toJson(float[] embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize embedding", ex);
        }
    }

    public float[] fromJson(String json) {
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception ex) {
            return new float[DIMENSIONS];
        }
    }

    public double cosine(float[] left, float[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        int length = Math.min(left.length, right.length);
        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private float[] localEmbedding(String text) {
        float[] vector = new float[DIMENSIONS];
        Arrays.fill(vector, 0f);
        for (String token : text.split("[^a-z0-9]+")) {
            if (token.length() < 2) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[bucket] += 1.0f;
        }
        float norm = 0f;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm > 0f) {
            float scale = (float) (1.0d / Math.sqrt(norm));
            for (int i = 0; i < vector.length; i++) {
                vector[i] *= scale;
            }
        }
        return vector;
    }
}
