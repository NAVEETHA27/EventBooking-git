package com.eventbooking.security;

import io.jsonwebtoken.Claims;
import com.eventbooking.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for JwtTokenProvider token lifetime guarantees.
 *
 * Validates: Requirements 1.1, 1.2
 *
 * Property 1: Access token lifetime is exactly 15 minutes (exp - iat == 900000 ms)
 * Property 2: Refresh token lifetime is exactly 7 days (exp - iat == 604800000 ms)
 */
class JwtTokenProviderPropertyTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
            "TestSecretKeyForJwtTokenProviderPropertyTests2024!@#$",
            900_000L,
            604_800_000L,
            "college-events"
        );
    }

    // Property 1: Access token lifetime is exactly 15 minutes
    // Validates: Requirements 1.1
    @RepeatedTest(50)
    void property1_accessTokenLifetimeIsExactly15Minutes() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", "USER");
        Claims claims = jwtTokenProvider.extractClaims(token);
        long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(900_000L, lifetimeMs, "Access token lifetime must be exactly 900000 ms (15 min)");
    }

    // Property 2: Refresh token lifetime is exactly 7 days
    // Validates: Requirements 1.2
    @RepeatedTest(50)
    void property2_refreshTokenLifetimeIsExactly7Days() {
        String token = jwtTokenProvider.generateRefreshToken(1L, "test@example.com", "USER");
        Claims claims = jwtTokenProvider.extractClaims(token);
        long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(604_800_000L, lifetimeMs, "Refresh token lifetime must be exactly 604800000 ms (7 days)");
    }

    @Test
    void refreshTokenHasJtiClaim() {
        String token = jwtTokenProvider.generateRefreshToken(1L, "test@example.com", "USER");
        Claims claims = jwtTokenProvider.extractClaims(token);
        assertNotNull(claims.getId(), "Refresh token must have jti claim");
        assertFalse(claims.getId().isEmpty());
    }

    @Test
    void accessTokenHasIssClaim() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", "USER");
        assertEquals("college-events", jwtTokenProvider.extractIssuer(token));
    }

    // Property 1 (extended): Lifetime holds across multiple different users
    // Validates: Requirements 1.1
    @RepeatedTest(20)
    void property1_multipleUsersAccessTokensAllHave15MinLifetime() {
        long userId = (long) (Math.random() * 10000);
        String email = "user" + userId + "@example.com";
        String token = jwtTokenProvider.generateToken(userId, email, "USER");
        Claims claims = jwtTokenProvider.extractClaims(token);
        long lifetime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(900_000L, lifetime,
            "Access token lifetime must be exactly 900000 ms for any user");
    }
}
