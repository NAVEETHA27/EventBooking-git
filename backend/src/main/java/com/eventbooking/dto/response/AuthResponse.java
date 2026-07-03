package com.eventbooking.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
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
