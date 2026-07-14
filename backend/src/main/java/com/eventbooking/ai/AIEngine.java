package com.eventbooking.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized AI Engine — single façade for all AI operations.
 *
 * Every AI module (Recommendation, NLP, Travel, Sentiment, Career, etc.)
 * resolves its provider through this class instead of calling providers directly.
 *
 * Features:
 * - Provider selection with priority: configured preferred → any configured → null
 * - Simple in-memory prompt cache (keyed by hash) to reduce redundant API calls
 * - Prompt injection protection: strips dangerous instruction patterns
 * - Structured logging per call with token estimate
 * - Graceful null return when no provider is available
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AIEngine {

    private final List<AIProvider> providers;
    private final AIMetricsService metricsService;

    @Value("${ai.provider:gemini}")
    private String preferredProvider;

    /** Simple prompt-response cache — keyed by SHA-like hash, evicted on size limit */
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();
    private static final int CACHE_MAX = 200;

    // ── Core completion ───────────────────────────────────────────────────

    /**
     * Execute an AI completion.
     *
     * @param module  Logical module name for logging (e.g. "RECOMMENDATION", "TRAVEL")
     * @param system  System/persona prompt
     * @param user    User prompt
     * @param history Conversation history (may be empty)
     * @param cache   Whether to cache this response
     * @return AI-generated text, or null if no provider available
     */
    public String complete(String module, String system, String user,
                           List<com.eventbooking.dto.request.AIChatRequest.ChatMessage> history,
                           boolean cache) {
        String safe = sanitize(user);
        String key  = cacheKey(system, safe);

        if (cache && promptCache.containsKey(key)) {
            log.debug("[AIEngine][{}] Cache hit", module);
            metricsService.recordCacheHit();
            return promptCache.get(key);
        }

        if (cache) {
            metricsService.recordCacheMiss();
        }

        AIProvider provider = resolve();
        if (provider == null) {
            log.warn("[AIEngine][{}] No AI provider configured — returning null", module);
            metricsService.recordCall(module, 0, 0, 0, false);
            return null;
        }

        log.debug("[AIEngine][{}] provider={} promptLen≈{}", module, provider.name(), safe.length());
        long start = System.currentTimeMillis();
        int inputTokens = (safe.length() + (system != null ? system.length() : 0)) / 4;
        try {
            String result = provider.complete(system, safe, history);
            long latency = System.currentTimeMillis() - start;
            int outputTokens = result != null ? result.length() / 4 : 0;
            metricsService.recordCall(module, latency, inputTokens, outputTokens, result != null);

            if (result != null && cache) {
                if (promptCache.size() >= CACHE_MAX) promptCache.clear();
                promptCache.put(key, blockProtected(result));
            }
            return result;
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            log.warn("[AIEngine][{}] Completion failed: {}", module, ex.getMessage());
            metricsService.recordCall(module, latency, inputTokens, 0, false);
            return null;
        }
    }

    private String blockProtected(String s) {
        return s;
    }

    /**
     * Progressive token streaming execution.
     */
    public org.reactivestreams.Publisher<String> streamComplete(String module, String system, String user,
                                                                 List<com.eventbooking.dto.request.AIChatRequest.ChatMessage> history) {
        String safe = sanitize(user);
        AIProvider provider = resolve();
        if (provider == null) {
            log.warn("[AIEngine][{}] No AI provider configured — returning empty publisher", module);
            metricsService.recordCall(module + "_STREAM", 0, 0, 0, false);
            return reactor.core.publisher.Flux.empty();
        }

        log.debug("[AIEngine][{}] streaming provider={} promptLen≈{}", module, provider.name(), safe.length());
        long start = System.currentTimeMillis();
        int inputTokens = (safe.length() + (system != null ? system.length() : 0)) / 4;
        try {
            java.util.concurrent.atomic.AtomicInteger outputTokens = new java.util.concurrent.atomic.AtomicInteger(0);
            return reactor.core.publisher.Flux.from(provider.streamComplete(system, safe, history))
                    .doOnNext(token -> {
                        if (token != null) {
                            outputTokens.addAndGet(token.length() / 4 + 1);
                        }
                    })
                    .doOnComplete(() -> {
                        long latency = System.currentTimeMillis() - start;
                        metricsService.recordCall(module + "_STREAM", latency, inputTokens, outputTokens.get(), true);
                    })
                    .doOnError(err -> {
                        long latency = System.currentTimeMillis() - start;
                        metricsService.recordCall(module + "_STREAM", latency, inputTokens, outputTokens.get(), false);
                    });
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            log.warn("[AIEngine][{}] Streaming completion failed: {}", module, ex.getMessage());
            metricsService.recordCall(module + "_STREAM", latency, inputTokens, 0, false);
            return reactor.core.publisher.Flux.error(ex);
        }
    }

    /** Convenience — no history, no cache */
    public String complete(String module, String system, String user) {
        return complete(module, system, user, List.of(), false);
    }

    /** Convenience — no history, with cache */
    public String completeCached(String module, String system, String user) {
        return complete(module, system, user, List.of(), true);
    }

    // ── Provider resolution ───────────────────────────────────────────────

    public AIProvider resolve() {
        return providers.stream()
                .filter(p -> p.name().equalsIgnoreCase(preferredProvider) && p.isConfigured())
                .findFirst()
                .orElseGet(() -> providers.stream()
                        .filter(AIProvider::isConfigured).findFirst().orElse(null));
    }

    public boolean isAvailable() {
        return resolve() != null;
    }

    // ── Prompt safety ─────────────────────────────────────────────────────

    /**
     * Strip prompt injection patterns before sending to AI.
     * Removes "ignore previous instructions", "you are now", etc.
     */
    private String sanitize(String prompt) {
        if (prompt == null) return "";
        return prompt
                .replaceAll("(?i)ignore (all )?(previous|prior|above) instructions?", "[FILTERED]")
                .replaceAll("(?i)you are now", "[FILTERED]")
                .replaceAll("(?i)act as( a)?", "[FILTERED]")
                .replaceAll("(?i)disregard (your )?(rules|instructions|guidelines)", "[FILTERED]")
                .trim();
    }

    private String cacheKey(String system, String user) {
        return Integer.toHexString((system + "|" + user).hashCode());
    }
}
