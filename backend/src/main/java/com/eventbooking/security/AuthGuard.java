package com.eventbooking.security;

import com.eventbooking.exception.UnauthorizedException;

public final class AuthGuard {

    private AuthGuard() {
    }

    public static AuthPrincipal requirePrincipal(AuthPrincipal principal) {
        if (principal == null || principal.getId() == null || principal.getRole() == null || principal.getRole().isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }
        return principal;
    }
}
