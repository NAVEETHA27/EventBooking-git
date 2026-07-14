package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.EventCommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/events/{eventId}/community")
@RequiredArgsConstructor
public class EventCommunityController {

    private final EventCommunityService communityService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> context(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(communityService.context(eventId, principal)));
    }

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> messages(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(communityService.latest(eventId, limit, principal)));
    }

    @GetMapping("/messages/poll")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> poll(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") Long afterId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(communityService.newerThan(eventId, afterId, principal)));
    }

    @GetMapping("/messages/older")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> older(
            @PathVariable Long eventId,
            @RequestParam Long beforeId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(communityService.olderThan(eventId, beforeId, principal)));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> send(
            @PathVariable Long eventId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Map<String, Object> saved = communityService.send(
                eventId,
                principal,
                stringValue(body, "message", stringValue(body, "content", null)),
                stringValue(body, "messageType", "TEXT"),
                stringValue(body, "attachmentUrl", null),
                booleanValue(body, "announcement"),
                booleanValue(body, "pinned"));
        broadcast(eventId, "created", saved);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PostMapping("/messages/attachment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @PathVariable Long eventId,
            @RequestParam(required = false) String message,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal principal) throws java.io.IOException {
        Map<String, Object> saved = communityService.uploadAndSend(eventId, principal, message, file);
        broadcast(eventId, "created", saved);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> edit(
            @PathVariable Long eventId,
            @PathVariable Long messageId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Map<String, Object> saved = communityService.edit(eventId, messageId, principal, stringValue(body, "message", stringValue(body, "content", null)));
        broadcast(eventId, "updated", saved);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(
            @PathVariable Long eventId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Map<String, Object> saved = communityService.delete(eventId, messageId, principal);
        broadcast(eventId, "updated", saved);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PatchMapping("/messages/{messageId}/pin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pin(
            @PathVariable Long eventId,
            @PathVariable Long messageId,
            @RequestParam(defaultValue = "true") boolean pinned,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Map<String, Object> saved = communityService.pin(eventId, messageId, principal, pinned);
        broadcast(eventId, "updated", saved);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PatchMapping("/close")
    public ResponseEntity<ApiResponse<Void>> close(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "true") boolean closed,
            @AuthenticationPrincipal AuthPrincipal principal) {
        communityService.closeChat(eventId, principal, closed);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/mute")
    public ResponseEntity<ApiResponse<Void>> mute(
            @PathVariable Long eventId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        communityService.mute(eventId, longValue(body, "senderId"), stringValue(body, "senderRole", "USER"),
                intValue(body, "minutes", 30), principal);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @MessageMapping("/events/{eventId}/community")
    public void websocketMessage(
            @DestinationVariable Long eventId,
            @Payload Map<String, Object> body,
            Principal principal) {
        AuthPrincipal authPrincipal = authPrincipal(principal);
        Map<String, Object> saved = communityService.send(
                eventId,
                authPrincipal,
                stringValue(body, "message", stringValue(body, "content", null)),
                stringValue(body, "messageType", "TEXT"),
                stringValue(body, "attachmentUrl", null),
                booleanValue(body, "announcement"),
                booleanValue(body, "pinned"));
        broadcast(eventId, "created", saved);
    }

    private void broadcast(Long eventId, String action, Map<String, Object> message) {
        messagingTemplate.convertAndSend("/topic/events/" + eventId + "/community", Map.of(
                "action", action,
                "message", message
        ));
    }

    private AuthPrincipal authPrincipal(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.AbstractAuthenticationToken token
                && token.getPrincipal() instanceof AuthPrincipal authPrincipal) {
            return authPrincipal;
        }
        throw new AccessDeniedException("Unauthorized STOMP connection client");
    }

    private String stringValue(Map<String, Object> body, String key, String fallback) {
        Object value = body == null ? null : body.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean booleanValue(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    private Long longValue(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        if (value instanceof Number number) return number.longValue();
        return Long.valueOf(String.valueOf(value));
    }

    private int intValue(Map<String, Object> body, String key, int fallback) {
        Object value = body == null ? null : body.get(key);
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }
}
