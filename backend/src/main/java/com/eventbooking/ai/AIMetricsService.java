package com.eventbooking.ai;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AIMetricsService {

    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);
    private final AtomicLong totalTokensInput = new AtomicLong(0);
    private final AtomicLong totalTokensOutput = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    private final Map<String, AtomicLong> moduleCalls = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> moduleFailures = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> moduleLatencies = new ConcurrentHashMap<>();

    private static final int MAX_LATENCY_SAMPLES = 100;

    // Cost constants (Gemini API approximate: $0.075/1M input, $0.30/1M output tokens)
    private static final double INPUT_COST_PER_TOKEN = 0.075 / 1_000_000.0;
    private static final double OUTPUT_COST_PER_TOKEN = 0.30 / 1_000_000.0;

    public void recordCall(String module, long latencyMs, int inputTokens, int outputTokens, boolean success) {
        totalCalls.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);
        totalTokensInput.addAndGet(inputTokens);
        totalTokensOutput.addAndGet(outputTokens);

        moduleCalls.computeIfAbsent(module, k -> new AtomicLong(0)).incrementAndGet();

        if (!success) {
            totalFailures.incrementAndGet();
            moduleFailures.computeIfAbsent(module, k -> new AtomicLong(0)).incrementAndGet();
        }

        List<Long> latencies = moduleLatencies.computeIfAbsent(module, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (latencies) {
            if (latencies.size() >= MAX_LATENCY_SAMPLES) {
                latencies.remove(0);
            }
            latencies.add(latencyMs);
        }
    }

    public void recordCacheHit() {
        totalCacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        totalCacheMisses.incrementAndGet();
    }

    public Map<String, Object> getMetrics() {
        long calls = totalCalls.get();
        long failures = totalFailures.get();
        long hits = totalCacheHits.get();
        long misses = totalCacheMisses.get();
        long inputTokens = totalTokensInput.get();
        long outputTokens = totalTokensOutput.get();
        long latencySum = totalLatencyMs.get();

        double avgLatency = calls > 0 ? (double) latencySum / calls : 0.0;
        double successRate = calls > 0 ? ((double) (calls - failures) / calls) * 100.0 : 100.0;
        double cacheHitRate = (hits + misses) > 0 ? ((double) hits / (hits + misses)) * 100.0 : 0.0;
        double estimatedCost = (inputTokens * INPUT_COST_PER_TOKEN) + (outputTokens * OUTPUT_COST_PER_TOKEN);

        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("totalCalls", calls);
        metrics.put("totalFailures", failures);
        metrics.put("successRatePercent", Math.round(successRate * 100.0) / 100.0);
        metrics.put("cacheHits", hits);
        metrics.put("cacheMisses", misses);
        metrics.put("cacheHitRatePercent", Math.round(cacheHitRate * 100.0) / 100.0);
        metrics.put("totalTokensInput", inputTokens);
        metrics.put("totalTokensOutput", outputTokens);
        metrics.put("totalTokensCombined", inputTokens + outputTokens);
        metrics.put("estimatedCostUsd", Math.round(estimatedCost * 100000.0) / 100000.0);
        metrics.put("averageLatencyMs", Math.round(avgLatency * 100.0) / 100.0);

        Map<String, Long> callsByModule = new ConcurrentHashMap<>();
        moduleCalls.forEach((k, v) -> callsByModule.put(k, v.get()));
        metrics.put("callsByModule", callsByModule);

        Map<String, Long> failuresByModule = new ConcurrentHashMap<>();
        moduleFailures.forEach((k, v) -> failuresByModule.put(k, v.get()));
        metrics.put("failuresByModule", failuresByModule);

        Map<String, Double> avgLatencyByModule = new ConcurrentHashMap<>();
        moduleLatencies.forEach((k, list) -> {
            synchronized (list) {
                if (!list.isEmpty()) {
                    double sum = 0;
                    for (long val : list) sum += val;
                    avgLatencyByModule.put(k, Math.round((sum / list.size()) * 100.0) / 100.0);
                } else {
                    avgLatencyByModule.put(k, 0.0);
                }
            }
        });
        metrics.put("averageLatencyByModuleMs", avgLatencyByModule);

        return metrics;
    }
}
