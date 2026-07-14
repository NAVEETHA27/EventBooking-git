package com.eventbooking.config;

import com.eventbooking.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /* Explicit constructor — no Lombok needed, avoids blank-final-field compile error */
    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ── Public ────────────────────────────────────────────
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/auth/user/register",
                    "/auth/organizer/register",
                    "/auth/user/login",
                    "/auth/organizer/login",
                    "/auth/forgot-password",
                    "/auth/reset-password",
                    "/auth/reset-password/otp",
                    "/auth/refresh-token",
                    "/auth/otp/send",
                    "/auth/otp/verify",
                    "/chatbot",
                    "/ai/chat",
                    "/ai/copilot/chat",
                    "/ai/events/generate-description"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/auth/verify-email",
                    "/auth/test-email",
                    "/auth/session-status",
                    "/events",
                    "/events/featured",
                    "/events/categories",
                    "/help",
                    "/help/faqs",
                    "/help/videos",
                    "/ratings/events/*/summary",
                    "/ratings/events/*",
                    "/recommendations/discover",
                    "/certificates/verify/*",
                    "/organizer/analytics/leaderboard",
                    "/ai/search",
                    "/ai/chat/stream",
                    "/ai/chat/sessions",
                    "/ai/copilot/stream",
                    "/ai/copilot/sessions",
                    "/ai/copilot/persona",
                    "/ai/events/*/summary",
                    "/ai/events/*/travel",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/events/*").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/uploads/voice-messages/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/ws-native/**").permitAll()
                // ── Networking (authenticated users) ───────────────────
                .requestMatchers("/networking/**").hasRole("USER")
                // ── AI Insights (admin) + behavior (user) ─────────────
                .requestMatchers("/ai/insights/fraud").hasRole("ADMIN")
                .requestMatchers("/ai/insights/platform").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/ai/insights/behavior/**").hasRole("ADMIN")
                .requestMatchers("/ai/insights/behavior/me").hasRole("USER")
                // ── Organizer only ─────────────────────────────────────
                .requestMatchers("/organizer/**").hasRole("ORGANIZER")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // ── Authenticated ──────────────────────────────────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Token-Expiry"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
