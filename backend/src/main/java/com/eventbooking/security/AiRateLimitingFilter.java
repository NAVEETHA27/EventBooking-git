package com.eventbooking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to rate limit all AI operations under /api/ai/** and /ai/** endpoints.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiRateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        // Catch both prefixed and direct routes
        if (path.startsWith("/api/ai") || path.startsWith("/ai")) {
            String ip = request.getRemoteAddr();
            // Allow max 30 AI requests per minute per IP address
            if (!rateLimiterService.isAllowed("ai_ip:" + ip, 30, 60_000)) {
                log.warn("[RateLimit] AI request rate limit exceeded for IP={}", ip);
                response.setStatus(429); // HTTP 429 Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please wait a moment and try again.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
