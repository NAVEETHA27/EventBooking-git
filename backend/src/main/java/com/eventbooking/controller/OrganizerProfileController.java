package com.eventbooking.controller;

import com.eventbooking.dto.request.ProfileLocationRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.dto.response.OrganizerProfileResponse;
import com.eventbooking.dto.response.ProfileLocationResponse;
import com.eventbooking.entity.Participant;
import com.eventbooking.repository.ParticipantRepository;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.AnalyticsService;
import com.eventbooking.service.OrganizerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/organizer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORGANIZER')")
public class OrganizerProfileController {

    private final OrganizerProfileService  organizerProfileService;
    private final AnalyticsService         analyticsService;
    private final ParticipantRepository    participantRepository;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<OrganizerProfileResponse>> getProfile(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                organizerProfileService.getProfile(principal.getId())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<OrganizerProfileResponse>> updateProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                organizerProfileService.updateProfile(principal.getId(), body)));
    }

    @GetMapping("/profile/location")
    public ResponseEntity<ApiResponse<ProfileLocationResponse>> getLocation(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                organizerProfileService.getLocation(principal.getId())));
    }

    @PutMapping("/profile/location")
    public ResponseEntity<ApiResponse<ProfileLocationResponse>> updateLocation(
            @Valid @RequestBody ProfileLocationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Location updated",
                organizerProfileService.updateLocation(principal.getId(), request)));
    }

    @PostMapping("/profile/logo")
    public ResponseEntity<ApiResponse<String>> uploadLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Logo updated",
                organizerProfileService.uploadLogo(principal.getId(), file)));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        organizerProfileService.changePassword(
                principal.getId(),
                body.get("currentPassword"),
                body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getOrganizerDashboard(principal.getId())));
    }

    @GetMapping("/participants")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getParticipants(
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<Map<String, Object>> rows = participantRepository
                .findByEventOrganizerId(principal.getId())
                .stream()
                .map(p -> Map.<String, Object>of(
                        "id",        p.getId(),
                        "name",      p.getName(),
                        "email",     p.getEmail(),
                        "department", p.getDepartment() == null ? "" : p.getDepartment(),
                        "college",   p.getCollege() == null ? "" : p.getCollege(),
                        "bookingId", p.getBooking().getId(),
                        "ticketId",  p.getBooking().getTicketId(),
                        "eventName", p.getEvent().getEventName(),
                        "eventDate", p.getEvent().getEventDate().toString()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(rows));
    }
}
