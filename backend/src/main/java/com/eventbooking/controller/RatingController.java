package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.entity.EventRating;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
@Tag(name = "Ratings & Reviews", description = "Event ratings, reviews, and sentiment")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/events/{eventId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Submit a rating/review for an event (verified attendees only)")
    public ResponseEntity<ApiResponse<EventRating>> submitRating(
            @PathVariable Long eventId,
            @RequestBody EventRating rating,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted",
                        ratingService.submitRating(principal.getId(), eventId, rating)));
    }

    @PutMapping("/events/{eventId}/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Edit my rating/review for 7 days after submission")
    public ResponseEntity<ApiResponse<EventRating>> updateMyRating(
            @PathVariable Long eventId,
            @RequestBody EventRating rating,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Review updated",
                ratingService.updateRating(principal.getId(), eventId, rating)));
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get paginated reviews for an event")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getEventRatings(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.getEventRatings(eventId, page, size).map(ratingService::toPublicReview)));
    }

    @GetMapping("/events/{eventId}/summary")
    @Operation(summary = "Get rating summary + sentiment analysis for an event")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(ratingService.getEventRatingSummary(eventId)));
    }

    @GetMapping("/events/{eventId}/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my review for an event")
    public ResponseEntity<ApiResponse<EventRating>> getMyReview(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.getUserReview(principal.getId(), eventId).orElse(null)));
    }

    @GetMapping("/events/{eventId}/eligibility")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Check whether the current user can submit or edit feedback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eligibility(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.getFeedbackEligibility(principal.getId(), eventId)));
    }

    @PatchMapping("/{ratingId}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Moderate a review (Admin)")
    public ResponseEntity<ApiResponse<EventRating>> moderate(
            @PathVariable Long ratingId,
            @RequestParam EventRating.ModerationStatus status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.success("Review moderated",
                ratingService.moderateReview(ratingId, status, note)));
    }
}
