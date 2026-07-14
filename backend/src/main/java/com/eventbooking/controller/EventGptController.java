package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.EventGptPhaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai/eventgpt")
@RequiredArgsConstructor
public class EventGptController {
    private final EventGptPhaseService eventGptPhaseService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(ApiResponse.success(eventGptPhaseService.phaseStatus()));
    }

    @GetMapping("/events/{eventId}/travel-plan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> travelPlan(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(eventGptPhaseService.travelPlan(eventId)));
    }

    /**
     * Secured travel details — requires authentication.
     * Returns full travel info including private contact details (WhatsApp, emergency contacts).
     * Available to event organizer, admin, or registered confirmed participants.
     */
    @GetMapping("/events/{eventId}/travel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> travelDetails(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(eventGptPhaseService.travelPlan(eventId)));
    }

    @PostMapping("/events/{eventId}/qa")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eventQuestionAnswer(
            @PathVariable Long eventId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                eventGptPhaseService.eventQuestionAnswer(eventId, body.get("question"))));
    }

    @GetMapping("/events/{eventId}/packing-checklist")
    public ResponseEntity<ApiResponse<Map<String, Object>>> packingChecklist(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(eventGptPhaseService.packingChecklist(eventId)));
    }

    @PostMapping("/events/{eventId}/feedback-analysis")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeFeedback(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(eventGptPhaseService.analyzeFeedback(eventId)));
    }

    @PostMapping("/events/{eventId}/prediction")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> predict(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(eventGptPhaseService.predict(eventId, principal.getId())));
    }
}
