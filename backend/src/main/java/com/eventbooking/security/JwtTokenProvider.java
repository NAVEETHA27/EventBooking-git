package com.eventbooking.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Handles JWT creation, validation and claim extraction.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMs,
            @Value("${jwt.issuer}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.issuer = issuer;
    }

    public String generateToken(Long id, String email, String role) {
        return buildToken(id, email, role, expirationMs, false);
    }

    public String generateRefreshToken(Long id, String email, String role) {
        return buildToken(id, email, role, refreshExpirationMs, true);
    }

    private String buildToken(Long id, String email, String role, long ttl, boolean isRefresh) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .claim("id", id)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(secretKey);

        if (isRefresh) {
            builder.id(UUID.randomUUID().toString());
        }

        return builder.compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public Long extractId(String token) {
        return extractClaims(token).get("id", Long.class);
    }

    public String extractIssuer(String token) {
        return extractClaims(token).getIssuer();
    }

    public boolean isTokenExpired(String token) {
        try {
            extractClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
