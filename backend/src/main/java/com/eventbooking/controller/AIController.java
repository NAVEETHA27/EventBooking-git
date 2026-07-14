package com.eventbooking.controller;

import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.dto.response.AIChatResponse;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.AIService;
import com.eventbooking.service.BehavioralLearningService;
import com.eventbooking.service.ConversationalAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI chatbot, NLP search, event summary, description generator")
public class AIController {

    private final AIService aiService;
    private final BehavioralLearningService behavioralService;
    private final ConversationalAgentService agentService;

    @PostMapping("/chat")
    @Operation(summary = "Conversational AI chatbot with full event lifecycle support")
    public ResponseEntity<ApiResponse<AIChatResponse>> chat(
            @Valid @RequestBody AIChatRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                agentService.chat(request, principal, httpRequest.getRemoteAddr())));
    }

    @GetMapping("/search")
    @Operation(summary = "NLP natural language event search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> nlpSearch(
            @RequestParam String q,
            @AuthenticationPrincipal AuthPrincipal principal) {
        // Track search for behavioral learning (async — never blocks response)
        if (principal != null) {
            behavioralService.recordSearch(principal.getId(), q);
        }
        return ResponseEntity.ok(ApiResponse.success(
                aiService.nlpSearch(q, principal != null ? principal.getId() : null)));
    }

    @GetMapping("/events/{eventId}/summary")
    @Operation(summary = "AI-generated event summary (who should attend, agenda, outcomes)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> eventSummary(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(aiService.generateEventSummary(eventId)));
    }

    @PostMapping("/events/generate-description")
    @Operation(summary = "AI generates event description, agenda, FAQs, social captions from a prompt")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateDescription(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                aiService.generateEventDescription(body.getOrDefault("prompt", ""))));
    }

    @GetMapping("/career-guidance")
    @Operation(summary = "AI career path guidance based on participation history")
    public ResponseEntity<ApiResponse<String>> careerGuidance(
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) return ResponseEntity.ok(ApiResponse.success("Please log in for personalized career guidance."));
        return ResponseEntity.ok(ApiResponse.success(
                aiService.generateCareerGuidance(principal.getId())));
    }

    @GetMapping("/events/{eventId}/travel")
    @Operation(summary = "AI travel assistant — transport, hotels, weather for an event venue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> travelAssistant(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(
                aiService.generateTravelInfo(eventId)));
    }
}
