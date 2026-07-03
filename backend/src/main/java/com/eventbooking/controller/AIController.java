package com.eventbooking.controller;

import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.dto.response.AIChatResponse;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.AIService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {
    private final AIService aiService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AIChatResponse>> chat(
            @Valid @RequestBody AIChatRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(aiService.chat(request, principal, httpRequest.getRemoteAddr())));
    }
}
