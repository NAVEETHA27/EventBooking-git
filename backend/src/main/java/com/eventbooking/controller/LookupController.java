package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.dto.response.OrganizerProfileResponse;
import com.eventbooking.dto.response.UserProfileResponse;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.OrganizerRepository;
import com.eventbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/lookup")
@RequiredArgsConstructor
public class LookupController {
    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','USER')")
    public ApiResponse<Map<String, Object>> byPublicId(@PathVariable String code) {
        if (code.startsWith("U")) {
            var user = userRepository.findByUserCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("User ID not found"));
            return ApiResponse.success(Map.of("type", "USER", "profile", UserProfileResponse.from(user)));
        }
        if (code.startsWith("O")) {
            var organizer = organizerRepository.findByOrganizerCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Organizer ID not found"));
            return ApiResponse.success(Map.of("type", "ORGANIZER", "profile", OrganizerProfileResponse.from(organizer)));
        }
        throw new ResourceNotFoundException("ID not found");
    }
}
