package com.eventbooking.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store that tracks used refresh token JTIs to enforce
 * single-use rotation. Each entry maps a JTI → the timestamp (ms)
 * at which the token was redeemed. An hourly cleanup task removes
 * entries older than 7 days so the map does not grow unbounded.
 */
@Component
@Slf4j
public class RefreshTokenStore {

    /** JTI → redemption timestamp (System.currentTimeMillis()) */
    private final ConcurrentHashMap<String, Long> usedTokens = new ConcurrentHashMap<>();

    /** Refresh token lifetime: 7 days in milliseconds */
    private static final long REFRESH_TOKEN_TTL_MS = 604_800_000L;

    /**
     * Mark a JTI as used by recording the current time.
     *
     * @param jti the unique token identifier from the refresh token's {@code jti} claim
     */
    public void markUsed(String jti) {
        usedTokens.put(jti, System.currentTimeMillis());
        log.debug("Marked refresh token JTI as used: {}", jti);
    }

    /**
     * Check whether a JTI has already been redeemed.
     *
     * @param jti the unique token identifier to check
     * @return {@code true} if this JTI was previously passed to {@link #markUsed(String)}
     */
    public boolean isUsed(String jti) {
        return usedTokens.containsKey(jti);
    }

    /**
     * Remove entries older than 7 days. Runs automatically every hour
     * (3 600 000 ms). Relies on {@code @EnableScheduling} being active —
     * it is declared on {@link com.eventbooking.EventBookingApplication}.
     */
    @Scheduled(fixedRate = 3_600_000)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        int[] removed = {0};

        usedTokens.entrySet().removeIf(entry -> {
            boolean expired = now - entry.getValue() > REFRESH_TOKEN_TTL_MS;
            if (expired) removed[0]++;
            return expired;
        });

        if (removed[0] > 0) {
            log.info("RefreshTokenStore eviction: removed {} expired JTI(s); {} remaining",
                    removed[0], usedTokens.size());
        }
    }
}
