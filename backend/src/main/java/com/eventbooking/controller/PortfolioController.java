package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.entity.User;
import com.eventbooking.exception.BookingException;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
@Tag(name = "AI Student Portfolio", description = "AI-generated student portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my AI-generated student portfolio")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myPortfolio(
            @AuthenticationPrincipal AuthPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BookingException("User not found"));
        return ResponseEntity.ok(ApiResponse.success(
                portfolioService.getPortfolio(principal.getId(), user)));
    }
}
