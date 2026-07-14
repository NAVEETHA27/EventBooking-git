package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.EventAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/organizer/events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORGANIZER')")
public class EventAssetController {
    private final EventAssetService eventAssetService;

    @GetMapping("/{eventId}/participants/export")
    public ResponseEntity<byte[]> exportParticipants(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return eventAssetService.exportParticipants(eventId, principal.getId());
    }

    @PostMapping("/{eventId}/poster")
    public ResponseEntity<ApiResponse<String>> generatePoster(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Poster generated",
                eventAssetService.generatePoster(eventId, principal.getId())));
    }
}
