package com.eventbooking.config;

import com.eventbooking.security.AuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long started = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = System.currentTimeMillis() - started;
            String user = currentUser();
            if (response.getStatus() >= 500) {
                log.error("Request completed with server error method={} uri={} user={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), user, response.getStatus(), elapsedMs);
            } else {
                log.info("Request completed method={} uri={} user={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), user, response.getStatus(), elapsedMs);
            }
        }
    }

    private String currentUser() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return "anonymous";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipal authPrincipal) {
            return authPrincipal.getRole() + ":" + authPrincipal.getId();
        }
        return String.valueOf(principal);
    }
}
