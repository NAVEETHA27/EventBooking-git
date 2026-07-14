package com.eventbooking.controller;

import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.dto.response.AIChatResponse;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.ConversationalAgentService;
import com.eventbooking.service.ConversationMemoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Legacy /ai/chat endpoints — kept for backward compatibility.
 * New integrations should use /ai/copilot/* instead.
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final ConversationalAgentService agentService;
    private final ConversationMemoryService conversationMemoryService;
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @GetMapping("/chat/sessions")
    public ResponseEntity<ApiResponse<?>> sessions(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(conversationMemoryService.recentSessions(principal).stream()
                .map(session -> Map.of(
                        "sessionId", (Object) session.getSessionId(),
                        "title", session.getTitle() != null ? session.getTitle() : "Chat",
                        "updatedAt", session.getUpdatedAt()))
                .toList()));
    }

    /**
     * GET /api/ai/chat/stream — streaming SSE endpoint (legacy path).
     * Delegates to ConversationalAgentService for full agent pipeline support.
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {

        SseEmitter emitter = new SseEmitter(90_000L);
        streamExecutor.submit(() -> {
            try {
                AIChatResponse response = agentService.chat(
                        AIChatRequest.builder().message(message).sessionId(sessionId).build(),
                        principal,
                        httpRequest.getRemoteAddr());

                emitter.send(SseEmitter.event().name("session")
                        .data(response.getSessionId() != null ? response.getSessionId() : ""));

                // Send full answer as one token — word-splitting breaks spaces in SSE
                String answer = response.getAnswer();
                if (answer != null && !answer.isBlank()) {
                    emitter.send(SseEmitter.event().name("token").data(answer));
                }
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            } catch (Exception ex) {
                log.error("[ChatController] Stream error: {}", ex.getMessage(), ex);
                try {
                    emitter.send(SseEmitter.event().name("error").data("An error occurred. Please try again."));
                    emitter.complete();
                } catch (IOException ioEx) {
                    emitter.completeWithError(ioEx);
                }
            }
        });
        return emitter;
    }
}
