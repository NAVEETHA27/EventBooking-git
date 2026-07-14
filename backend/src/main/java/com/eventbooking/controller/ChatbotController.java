package com.eventbooking.controller;

import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.dto.response.AIChatResponse;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.ConversationalAgentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Legacy /chatbot endpoint — backward compatible.
 * Delegates to ConversationalAgentService for full agent support.
 */
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    private final ConversationalAgentService agentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatbotResponse>> ask(
            @RequestBody ChatbotRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        AIChatResponse ai = agentService.chat(
                AIChatRequest.builder().message(request != null ? request.getMessage() : "").build(),
                principal,
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(ChatbotResponse.of(ai.getAnswer())));
    }

    @Getter @Setter
    public static class ChatbotRequest {
        private String message;
    }

    @Getter @Setter
    public static class ChatbotResponse {
        private String answer;
        public static ChatbotResponse of(String answer) {
            ChatbotResponse response = new ChatbotResponse();
            response.setAnswer(answer);
            return response;
        }
    }
}
