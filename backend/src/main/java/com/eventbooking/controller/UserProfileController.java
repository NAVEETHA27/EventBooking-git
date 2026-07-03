package com.eventbooking.controller;

import com.eventbooking.dto.request.ProfileLocationRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.dto.response.ProfileLocationResponse;
import com.eventbooking.dto.response.UserProfileResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                userProfileService.getProfile(principal.getId())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                userProfileService.updateProfile(principal.getId(), body)));
    }

    @GetMapping("/profile/location")
    public ResponseEntity<ApiResponse<ProfileLocationResponse>> getLocation(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                userProfileService.getLocation(principal.getId())));
    }

    @PutMapping("/profile/location")
    public ResponseEntity<ApiResponse<ProfileLocationResponse>> updateLocation(
            @Valid @RequestBody ProfileLocationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Location updated",
                userProfileService.updateLocation(principal.getId(), request)));
    }

    @PostMapping("/profile/picture")
    public ResponseEntity<ApiResponse<String>> uploadPicture(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated",
                userProfileService.uploadPicture(principal.getId(), file)));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        userProfileService.changePassword(
                principal.getId(),
                body.get("currentPassword"),
                body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
