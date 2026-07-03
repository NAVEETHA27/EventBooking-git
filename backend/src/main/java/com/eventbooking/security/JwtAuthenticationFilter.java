package com.eventbooking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Intercepts every request, validates the Bearer JWT and sets Spring Security context.
 * Returns structured 401 JSON responses for expired tokens (TOKEN_EXPIRED)
 * and tokens with an invalid issuer claim (INVALID_ISSUER).
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final String expectedIssuer;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   @Value("${jwt.issuer}") String expectedIssuer) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.expectedIssuer = expectedIssuer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {

            // 1. Check for expiry first — gives a specific, actionable error code
            if (jwtTokenProvider.isTokenExpired(token)) {
                writeJsonError(response, request,
                        "TOKEN_EXPIRED", "Access token expired");
                return; // do NOT continue the filter chain
            }

            // 2. Validate signature, structure, and claims (non-expiry errors)
            if (jwtTokenProvider.validateToken(token)) {
                try {
                    // 3. Validate the issuer claim (Requirement 9.7)
                    String tokenIssuer = jwtTokenProvider.extractIssuer(token);
                    if (!expectedIssuer.equals(tokenIssuer)) {
                        log.warn("JWT issuer mismatch — expected: {}, got: {}", expectedIssuer, tokenIssuer);
                        writeJsonError(response, request,
                                "INVALID_ISSUER", "Invalid token issuer");
                        return; // do NOT continue the filter chain
                    }

                    // 4. Token is valid — populate the security context
                    String email = jwtTokenProvider.extractEmail(token);
                    String role  = jwtTokenProvider.extractRole(token);
                    Long   id    = jwtTokenProvider.extractId(token);

                    var authority = new SimpleGrantedAuthority("ROLE_" + role);
                    var auth = new UsernamePasswordAuthenticationToken(
                            new AuthPrincipal(id, email, role),
                            null,
                            List.of(authority));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                } catch (Exception ex) {
                    log.error("Could not set user authentication: {}", ex.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Writes a structured JSON 401 error response and halts filter chain processing.
     *
     * @param response  the HTTP response to write to
     * @param request   the current HTTP request (used for the path field)
     * @param errorCode machine-readable error code, e.g. TOKEN_EXPIRED or INVALID_ISSUER
     * @param message   human-readable error description
     */
    private void writeJsonError(HttpServletResponse response,
                                HttpServletRequest request,
                                String errorCode,
                                String message) throws IOException {
        String timestamp = Instant.now().toString();
        String path = request.getRequestURI();

        String json = String.format(
                "{\"errorCode\":\"%s\",\"status\":401,\"message\":\"%s\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
                errorCode, message, timestamp, path);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        if (request.getRequestURI().endsWith("/notifications/stream")) {
            return request.getParameter("token");
        }
        return null;
    }
}
