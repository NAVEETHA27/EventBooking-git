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
                    "/ai/chat"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/auth/verify-email",
                    "/auth/test-email",
                    "/events",
                    "/events/featured",
                    "/events/categories",
                    "/help",
                    "/help/faqs",
                    "/help/videos",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/events/*").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/uploads/**").permitAll()
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
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
