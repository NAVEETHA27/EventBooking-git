package com.eventbooking.security;

import lombok.*;

/**
 * Holds the authenticated principal details stored in SecurityContext.
 */
@Getter
@AllArgsConstructor
public class AuthPrincipal {
    private final Long id;
    private final String email;
    private final String role;
}
