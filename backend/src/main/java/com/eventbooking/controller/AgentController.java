package com.eventbooking.controller;

import com.eventbooking.dto.request.AgentToolRequest;
import com.eventbooking.dto.response.AgentToolResponse;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.EventGptToolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/agent")
@RequiredArgsConstructor
public class AgentController {
    private final EventGptToolService toolService;

    @GetMapping("/tools")
    public ResponseEntity<ApiResponse<List<String>>> tools() {
        return ResponseEntity.ok(ApiResponse.success(toolService.availableTools()));
    }

    @PostMapping("/invoke")
    public ResponseEntity<ApiResponse<AgentToolResponse>> invoke(
            @Valid @RequestBody AgentToolRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(toolService.invoke(request, principal)));
    }
}
