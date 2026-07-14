package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.entity.Event;
import com.eventbooking.repository.*;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.BehavioralLearningService;
import com.eventbooking.service.FraudDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 16 — AI Insights Dashboard + Phase 10 Fraud Detection API.
 *
 * Admin endpoints exposing platform-wide analytics for the Insights dashboard:
 * heatmaps, growth, department participation, retention, revenue, fraud reports.
 */
@RestController
@RequestMapping("/ai/insights")
@RequiredArgsConstructor
@Tag(name = "AI Insights", description = "Platform-wide AI insights and fraud detection")
public class AIInsightsController {

    private final EventRepository       eventRepository;
    private final BookingRepository     bookingRepository;
    private final UserRepository        userRepository;
    private final AttendanceRepository  attendanceRepository;
    private final EventRatingRepository ratingRepository;
    private final FraudDetectionService fraudService;
    private final BehavioralLearningService behavioralService;

    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Full platform AI insights dashboard — growth, heatmap, retention, revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> platformInsights() {
        Map<String, Object> insights = new LinkedHashMap<>();

        // ── Event growth (last 12 months) ────────────────────────────────
        List<Event> allEvents = eventRepository.findAll();
        Map<String, Long> monthlyGrowth = allEvents.stream()
                .filter(e -> e.getCreatedAt() != null
                          && e.getCreatedAt().isAfter(LocalDate.now().minusMonths(12).atStartOfDay()))
                .collect(Collectors.groupingBy(
                        e -> e.getCreatedAt().getYear() + "-"
                             + String.format("%02d", e.getCreatedAt().getMonthValue()),
                        Collectors.counting()));
        insights.put("eventGrowthByMonth", monthlyGrowth);

        // ── Department participation heatmap ─────────────────────────────
        Map<String, Long> deptParticipation = allEvents.stream()
                .filter(e -> e.getDepartmentName() != null)
                .collect(Collectors.groupingBy(Event::getDepartmentName, Collectors.counting()));
        insights.put("departmentParticipation", deptParticipation);

        // ── College participation leaderboard ─────────────────────────────
        List<Object[]> collegeRaw = bookingRepository.findCollegeParticipation(
                PageRequest.of(0, 20));
        List<Map<String, Object>> collegeLeaderboard = collegeRaw.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("college",      row[0] != null ? row[0] : "Unknown");
            m.put("participants", ((Number) row[1]).longValue());
            return m;
        }).collect(Collectors.toList());
        insights.put("collegeLeaderboard", collegeLeaderboard);

        // ── Category distribution ─────────────────────────────────────────
        Map<String, Long> catDist = allEvents.stream()
                .filter(e -> e.getCategory() != null)
                .collect(Collectors.groupingBy(Event::getCategory, Collectors.counting()));
        insights.put("categoryDistribution", catDist);

        // ── User growth ───────────────────────────────────────────────────
        long totalUsers  = userRepository.count();
        insights.put("totalUsers",  totalUsers);
        insights.put("totalEvents", allEvents.size());

        // ── Rating distribution ────────────────────────────────────────────
        Map<Integer, Long> ratingDist = ratingRepository.findAll().stream()
                .collect(Collectors.groupingBy(r -> r.getOverallRating(), Collectors.counting()));
        insights.put("ratingDistribution", ratingDist);

        // ── Top rated events ──────────────────────────────────────────────
        List<Map<String, Object>> topRated = allEvents.stream()
                .map(e -> {
                    double avg = ratingRepository.findAverageRatingByEventId(e.getId()).orElse(0.0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("eventId",       e.getId());
                    m.put("eventName",     e.getEventName());
                    m.put("avgRating",     Math.round(avg * 10.0) / 10.0);
                    m.put("category",      e.getCategory());
                    return m;
                })
                .filter(m -> ((Number) m.get("avgRating")).doubleValue() > 0)
                .sorted((a, b) -> Double.compare(
                        ((Number) b.get("avgRating")).doubleValue(),
                        ((Number) a.get("avgRating")).doubleValue()))
                .limit(10)
                .collect(Collectors.toList());
        insights.put("topRatedEvents", topRated);

        insights.put("generatedAt", LocalDate.now().toString());
        return ResponseEntity.ok(ApiResponse.success(insights));
    }

    @GetMapping("/fraud")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "AI fraud detection report — duplicate accounts, bot bookings, fake reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fraudReport() {
        return ResponseEntity.ok(ApiResponse.success(fraudService.generateFraudReport()));
    }

    @PostMapping("/behavior/refresh/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger behavioral profile refresh for a specific user")
    public ResponseEntity<ApiResponse<String>> refreshBehavior(@PathVariable Long userId) {
        behavioralService.refreshProfileAsync(userId);
        return ResponseEntity.ok(ApiResponse.success("Behavioral profile refresh queued for userId=" + userId));
    }

    @GetMapping("/behavior/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my AI behavioral summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myBehavior(
            @AuthenticationPrincipal AuthPrincipal principal) {
        // Trigger a refresh and return current summary
        behavioralService.refreshProfileAsync(principal.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Behavioral profile refresh queued. Check your portfolio for the updated AI summary.");
        result.put("userId",  principal.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
