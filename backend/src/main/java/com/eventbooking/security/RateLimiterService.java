package com.eventbooking.security;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe sliding window in-memory rate limiter for security critical operations (OTP, Login, AI).
 */
@Service
public class RateLimiterService {

    private final Map<String, List<Long>> requestsMap = new ConcurrentHashMap<>();

    /**
     * Checks if the request is within rate limits.
     *
     * @param key        Unique identifier (e.g., IP, Email, User ID) combined with endpoint
     * @param limit      Max requests allowed
     * @param durationMs Sliding window duration in milliseconds
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String key, int limit, long durationMs) {
        long now = System.currentTimeMillis();
        List<Long> timestamps = requestsMap.computeIfAbsent(key, k -> new ArrayList<>());

        synchronized (timestamps) {
            timestamps.removeIf(t -> now - t > durationMs);
            if (timestamps.size() >= limit) {
                return false;
            }
            timestamps.add(now);
            return true;
        }
    }
}
