package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.entity.NetworkingConnection;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.NetworkingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Phase 10 — AI Networking endpoints.
 */
@RestController
@RequestMapping("/networking")
@RequiredArgsConstructor
@Tag(name = "AI Networking", description = "Smart peer connection recommendations")
public class NetworkingController {

    private final NetworkingService networkingService;

    @GetMapping("/suggestions")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "AI-suggested peer connections based on department, skills, and mutual events")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> suggestions(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                networkingService.suggestConnections(principal.getId())));
    }

    @PostMapping("/connect/{receiverId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Send a connection request to another user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> connect(
            @PathVariable Long receiverId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                networkingService.sendConnectionRequest(principal.getId(), receiverId)));
    }

    @PatchMapping("/{connectionId}/respond")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Accept or reject a pending connection request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> respond(
            @PathVariable Long connectionId,
            @RequestParam NetworkingConnection.ConnectionStatus status,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                networkingService.respondToConnection(connectionId, principal.getId(), status)));
    }

    @GetMapping("/my-connections")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all accepted connections")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> myConnections(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                networkingService.getMyConnections(principal.getId())));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get pending incoming connection requests")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> pending(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                networkingService.getPendingRequests(principal.getId())));
    }
}
