package com.eventbooking.controller;

import com.eventbooking.dto.request.ApprovalDecisionRequest;
import com.eventbooking.dto.request.RefundStatusUpdateRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.dto.response.EventResponse;
import com.eventbooking.dto.response.RefundResponse;
import com.eventbooking.entity.ApprovalRequest;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.AdminDashboardService;
import com.eventbooking.service.EventService;
import com.eventbooking.payment.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final EventService          eventService;
    private final RefundService         refundService;
    private final com.eventbooking.ai.AIMetricsService aiMetricsService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(dashboardService.stats());
    }

    @GetMapping("/ai-metrics")
    public ApiResponse<Map<String, Object>> aiMetrics() {
        return ApiResponse.success(aiMetricsService.getMetrics());
    }

    @GetMapping("/approvals")
    public ApiResponse<Page<ApprovalRequest>> approvals(
            @RequestParam(required = false) ApprovalRequest.ApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(dashboardService.approvals(status, page, size));
    }

    @PostMapping("/events/{eventId}/review")
    public ApiResponse<EventResponse> reviewEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Review saved",
                eventService.reviewEvent(eventId, principal.getId(),
                        request.getDecision(), request.getReason()));
    }

    @GetMapping("/users")
    public ApiResponse<?> users(Pageable pageable) {
        return ApiResponse.success(dashboardService.users(pageable));
    }

    @GetMapping("/organizers")
    public ApiResponse<?> organizers(Pageable pageable) {
        return ApiResponse.success(dashboardService.organizers(pageable));
    }

    @GetMapping("/events")
    public ApiResponse<?> events(Pageable pageable) {
        return ApiResponse.success(dashboardService.events(pageable));
    }

    @GetMapping("/payments")
    public ApiResponse<?> payments(Pageable pageable) {
        return ApiResponse.success(dashboardService.payments(pageable));
    }

    @GetMapping("/refunds")
    public ApiResponse<?> refunds(Pageable pageable) {
        return ApiResponse.success(dashboardService.refunds(pageable));
    }

    @PatchMapping("/refunds/{refundId}/status")
    public ApiResponse<RefundResponse> updateRefundStatus(
            @PathVariable Long refundId,
            @Valid @RequestBody RefundStatusUpdateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Refund status updated",
                refundService.updateStatus(refundId, request.getStatus(),
                        principal.getId(), request.getNote()));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<?> auditLogs(Pageable pageable) {
        return ApiResponse.success(dashboardService.auditLogs(pageable));
    }
}
