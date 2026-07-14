package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class DirectMessageController {

    private final DirectMessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    /** Send message to a connected user */
    @PostMapping("/{receiverId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> send(
            @PathVariable Long receiverId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Map<String, Object> saved = messageService.send(principal.getId(), receiverId, body.get("content"));
        broadcast(saved, "created");
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    /** Load full conversation */
    @GetMapping("/{otherId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> conversation(
            @PathVariable Long otherId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                messageService.getConversation(principal.getId(), otherId)));
    }

    /** Poll for new messages (long polling) */
    @GetMapping("/{otherId}/poll")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> poll(
            @PathVariable Long otherId,
            @RequestParam(defaultValue = "0") Long lastId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                messageService.poll(principal.getId(), otherId, lastId)));
    }

    /** Unread message count */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unread(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("unread", messageService.unreadCount(principal.getId()))));
    }

    /** Send a voice message */
    @PostMapping("/{receiverId}/voice")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendVoice(
            @PathVariable Long receiverId,
            @RequestPart("audio") MultipartFile audioFile,
            @RequestParam(required = false, defaultValue = "0") Integer duration,
            @AuthenticationPrincipal AuthPrincipal principal) throws IOException {
        Map<String, Object> saved = messageService.sendVoice(principal.getId(), receiverId, audioFile, duration);
        broadcast(saved, "created");
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> edit(
            @PathVariable Long messageId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Map<String, Object> saved = messageService.edit(principal.getId(), messageId, body.get("content"));
        broadcast(saved, "updated");
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteForMe(
            @PathVariable Long messageId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Map<String, Object> saved = messageService.deleteForMe(principal.getId(), messageId);
        messagingTemplate.convertAndSend("/queue/users/" + principal.getId() + "/chat", Map.of("action", "deletedForMe", "message", saved));
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/messages/{messageId}/everyone")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unsend(
            @PathVariable Long messageId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Map<String, Object> saved = messageService.unsend(principal.getId(), messageId);
        broadcast(saved, "updated");
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    private void broadcast(Map<String, Object> message, String action) {
        Object senderId = message.get("senderId");
        Object receiverId = message.get("receiverId");
        Map<String, Object> payload = Map.of("action", action, "message", message);
        messagingTemplate.convertAndSend("/queue/users/" + senderId + "/chat", payload);
        messagingTemplate.convertAndSend("/queue/users/" + receiverId + "/chat", payload);
    }
}
