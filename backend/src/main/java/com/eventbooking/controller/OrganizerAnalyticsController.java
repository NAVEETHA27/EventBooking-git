package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.OrganizerAnalyticsService;
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
@RequestMapping("/organizer/analytics")
@RequiredArgsConstructor
@Tag(name = "Organizer Analytics", description = "AI-powered organizer dashboard analytics")
public class OrganizerAnalyticsController {

    private final OrganizerAnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Get full organizer analytics with AI insights")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analytics(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getOrganizerAnalytics(principal.getId())));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "College leaderboard (public)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> leaderboard(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getCollegeLeaderboard(limit)));
    }

    @GetMapping("/ask")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Natural-language analytics query — ask questions about your events in plain English")
    public ResponseEntity<ApiResponse<String>> ask(
            @RequestParam String q,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.answerNaturalLanguageQuery(principal.getId(), q)));
    }

    @GetMapping("/predict/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "AI prediction: registrations, attendance, revenue, no-shows for a specific event")
    public ResponseEntity<ApiResponse<Map<String, Object>>> predict(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.predictEventOutcomes(eventId, principal.getId())));
    }
}
