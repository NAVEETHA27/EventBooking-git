package com.eventbooking.controller;

import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.dto.response.AIChatResponse;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.ConversationalAgentService;
import com.eventbooking.service.ConversationMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Copilot Controller — unified endpoint for:
 * - Organizer AI Copilot  (role=ORGANIZER)
 * - Student AI Assistant  (role=USER / guest)
 *
 * The same endpoint auto-adapts its persona and tool access based on the
 * authenticated principal's role. The frontend uses the role to render the
 * appropriate UI (e.g. "EventCopilot" sidebar vs "EventBot" chat widget).
 */
@RestController
@RequestMapping("/ai/copilot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Copilot", description = "Conversational AI agents for Organizers and Students")
public class CopilotController {

    private final ConversationalAgentService agentService;
    private final ConversationMemoryService memoryService;

    /**
     * POST /api/ai/copilot/chat
     *
     * Full-response chat (non-streaming). Returns the complete answer in one response.
     * Use this for simple integrations or when streaming is not supported.
     */
    @PostMapping("/chat")
    @Operation(summary = "Conversational AI agent chat (full response)")
    public ResponseEntity<ApiResponse<AIChatResponse>> chat(
            @Valid @RequestBody AIChatRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        AIChatResponse response = agentService.chat(request, principal, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Streaming chat via Server-Sent Events")
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {

        SseEmitter emitter = new SseEmitter(90_000L); // 90s timeout
        AIChatRequest request = AIChatRequest.builder()
                .message(message)
                .sessionId(sessionId)
                .build();

        String resolvedSessionId = sessionId != null && !sessionId.isBlank() ? sessionId : java.util.UUID.randomUUID().toString();
        try {
            emitter.send(SseEmitter.event().name("session").data(resolvedSessionId));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        org.reactivestreams.Publisher<String> tokenStream = agentService.chatStream(request, principal, httpRequest.getRemoteAddr());

        reactor.core.publisher.Flux.from(tokenStream)
                .subscribe(
                        token -> {
                            try {
                                emitter.send(SseEmitter.event().name("token").data(token));
                            } catch (IOException ex) {
                                log.warn("[CopilotController] SSE connection closed by client");
                            }
                        },
                        error -> {
                            log.error("[CopilotController] SSE stream error: {}", error.getMessage());
                            try {
                                emitter.send(SseEmitter.event().name("error").data("Something went wrong. Please try again."));
                                emitter.complete();
                            } catch (IOException ex) {
                                emitter.completeWithError(ex);
                            }
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                emitter.complete();
                            } catch (IOException ex) {
                                emitter.completeWithError(ex);
                            }
                        }
                );

        return emitter;
    }

    /**
     * GET /api/ai/copilot/sessions
     *
     * Lists recent chat sessions for the authenticated user.
     */
    @GetMapping("/sessions")
    @Operation(summary = "List recent chat sessions")
    public ResponseEntity<ApiResponse<?>> sessions(@AuthenticationPrincipal AuthPrincipal principal) {
        var sessions = memoryService.recentSessions(principal).stream()
                .map(s -> Map.of(
                        "sessionId", (Object) s.getSessionId(),
                        "title", s.getTitle() != null ? s.getTitle() : "Chat",
                        "updatedAt", s.getUpdatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * GET /api/ai/copilot/persona
     *
     * Returns the persona/mode the current user will interact with.
     * Useful for the frontend to render the correct AI assistant UI.
     */
    @GetMapping("/persona")
    @Operation(summary = "Get current AI persona based on user role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> persona(
            @AuthenticationPrincipal AuthPrincipal principal) {
        String role = principal != null ? principal.getRole() : "GUEST";
        Map<String, Object> persona = switch (role.toUpperCase()) {
            case "ORGANIZER" -> Map.of(
                    "name", "EventCopilot",
                    "role", "ORGANIZER",
                    "description", "Your AI-powered event management copilot",
                    "capabilities", java.util.List.of(
                            "Analytics & revenue insights",
                            "Participant management",
                            "Certificate generation",
                            "Event predictions",
                            "Feedback sentiment analysis",
                            "Email & notification management"
                    ),
                    "avatar", "🎯"
            );
            case "USER" -> Map.of(
                    "name", "EventBot",
                    "role", "USER",
                    "description", "Your personal event discovery assistant",
                    "capabilities", java.util.List.of(
                            "Event discovery & recommendations",
                            "Booking & ticket management",
                            "Payment & refund status",
                            "Certificate downloads",
                            "Travel directions to venues",
                            "Career guidance"
                    ),
                    "avatar", "🎓"
            );
            default -> Map.of(
                    "name", "EventBot",
                    "role", "GUEST",
                    "description", "Discover events and get answers",
                    "capabilities", java.util.List.of(
                            "Browse upcoming events",
                            "Platform FAQ",
                            "Event details"
                    ),
                    "avatar", "🎫"
            );
        };
        return ResponseEntity.ok(ApiResponse.success(persona));
    }
}
