package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.entity.UserInterest;
import com.eventbooking.entity.User;
import com.eventbooking.exception.BookingException;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Recommendations", description = "Personalized event recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping("/discover")
    @Operation(summary = "Get all discover page categories (Trending, AI Picks, Near You…)")
    public ResponseEntity<ApiResponse<Map<String, List<Map<String, Object>>>>> discover(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getDiscoverRecommendations(userId, lat, lon)));
    }

    @GetMapping("/interests")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my interest profile")
    public ResponseEntity<ApiResponse<UserInterest>> getInterests(
            @AuthenticationPrincipal AuthPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BookingException("User not found"));
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getOrCreateInterest(principal.getId(), user)));
    }

    @PutMapping("/interests")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update my interest profile for better recommendations")
    public ResponseEntity<ApiResponse<UserInterest>> updateInterests(
            @RequestBody UserInterest interest,
            @AuthenticationPrincipal AuthPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BookingException("User not found"));
        interest.setUser(user);
        return ResponseEntity.ok(ApiResponse.success("Interests updated",
                recommendationService.updateInterest(principal.getId(), interest)));
    }
}
