package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.entity.mongo.Notification;
import com.eventbooking.security.AuthGuard;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.*;

/**
 * GET   /api/notifications          – paginated notifications
 * GET   /api/notifications/unread   – unread count
 * PATCH /api/notifications/read-all – mark all read
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Notification>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        principal = AuthGuard.requirePrincipal(principal);
        String type = principal.getRole();
        Page<Notification> notifications =
                notificationService.getNotifications(principal.getId(), type, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal AuthPrincipal principal) {
        principal = AuthGuard.requirePrincipal(principal);
        long count = notificationService.getUnreadCount(principal.getId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal AuthPrincipal principal) {
        principal = AuthGuard.requirePrincipal(principal);
        notificationService.markAllRead(principal.getId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @GetMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal AuthPrincipal principal) {
        principal = AuthGuard.requirePrincipal(principal);
        return notificationService.stream(principal.getId(), principal.getRole());
    }
}
