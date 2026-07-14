package com.eventbooking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    /** Milliseconds until access token expires (e.g. 1800000 = 30 min) */
    private long expiresIn;
    /**
     * Absolute UTC timestamp when the access token expires.
     * Frontend uses this to schedule auto-logout without polling.
     * Format: epoch millis (e.g. 1720444800000)
     */
    private long expiresAt;
    private UserInfo user;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private String role;
        private String profilePicture;
        private boolean emailVerified;
    }
}
