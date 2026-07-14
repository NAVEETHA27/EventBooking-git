package com.eventbooking.service;

import com.eventbooking.entity.DirectMessage;
import com.eventbooking.entity.NetworkingConnection;
import com.eventbooking.entity.User;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.exception.UnauthorizedException;
import com.eventbooking.repository.DirectMessageRepository;
import com.eventbooking.repository.NetworkingConnectionRepository;
import com.eventbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectMessageService {
    private static final Duration UNSEND_WINDOW = Duration.ofMinutes(15);

    private final DirectMessageRepository messageRepository;
    private final NetworkingConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    /** Send a text message — only accepted connections can chat */
    @Transactional
    public Map<String, Object> send(Long senderId, Long receiverId, String content) {
        if (!StringUtils.hasText(content) || content.length() > 2000)
            throw new IllegalArgumentException("Message must be 1–2000 characters");
        assertConnected(senderId, receiverId);
        User sender   = loadUser(senderId);
        User receiver = loadUser(receiverId);
        DirectMessage msg = DirectMessage.builder()
                .sender(sender).receiver(receiver)
                .content(content.trim()).messageType("TEXT")
                .build();
        return toMap(messageRepository.save(msg));
    }

    /** Send a voice message — upload audio file and store URL */
    @Transactional
    public Map<String, Object> sendVoice(Long senderId, Long receiverId,
                                          MultipartFile audioFile, Integer durationSeconds) throws IOException {
        assertConnected(senderId, receiverId);
        String contentType = audioFile.getContentType();
        if (contentType == null || (!contentType.startsWith("audio/") && !contentType.equals("application/octet-stream"))) {
            throw new IllegalArgumentException("Only audio files are allowed for voice messages");
        }
        if (audioFile.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Voice message must be under 10 MB");
        }
        User sender   = loadUser(senderId);
        User receiver = loadUser(receiverId);
        String voiceUrl = storageService.store("voice-messages", audioFile, "voice_" + senderId);
        DirectMessage msg = DirectMessage.builder()
                .sender(sender).receiver(receiver)
                .content("🎤 Voice message")
                .messageType("VOICE")
                .voiceUrl(voiceUrl)
                .voiceDurationSeconds(durationSeconds)
                .build();
        return toMap(messageRepository.save(msg));
    }

    /** Load full conversation history */
    @Transactional
    public List<Map<String, Object>> getConversation(Long userId, Long otherId) {
        assertConnected(userId, otherId);
        messageRepository.markRead(otherId, userId);
        return messageRepository.findConversation(userId, otherId).stream()
                .filter(message -> visibleTo(message, userId))
                .map(this::toMap).collect(Collectors.toList());
    }

    /** Long-poll: messages newer than lastId */
    @Transactional
    public List<Map<String, Object>> poll(Long userId, Long otherId, Long lastId) {
        assertConnected(userId, otherId);
        List<Map<String, Object>> msgs = messageRepository.pollNew(userId, otherId, lastId)
                .stream().filter(message -> visibleTo(message, userId))
                .map(this::toMap).collect(Collectors.toList());
        messageRepository.markRead(otherId, userId);
        return msgs;
    }

    @Transactional
    public Map<String, Object> edit(Long userId, Long messageId, String content) {
        DirectMessage message = loadMessageForParticipant(messageId, userId);
        if (!message.getSender().getId().equals(userId)) {
            throw new UnauthorizedException("Only the sender can edit this message");
        }
        if (message.isDeletedForEveryone()) {
            throw new IllegalArgumentException("Deleted messages cannot be edited");
        }
        if (!StringUtils.hasText(content) || content.length() > 2000) {
            throw new IllegalArgumentException("Message must be 1-2000 characters");
        }
        message.setContent(content.trim());
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        return toMap(messageRepository.save(message));
    }

    @Transactional
    public Map<String, Object> deleteForMe(Long userId, Long messageId) {
        DirectMessage message = loadMessageForParticipant(messageId, userId);
        if (message.getSender().getId().equals(userId)) {
            message.setDeletedForSender(true);
        } else {
            message.setDeletedForReceiver(true);
        }
        message.setDeletedAt(LocalDateTime.now());
        return toMap(messageRepository.save(message));
    }

    @Transactional
    public Map<String, Object> unsend(Long userId, Long messageId) {
        DirectMessage message = loadMessageForParticipant(messageId, userId);
        if (!message.getSender().getId().equals(userId)) {
            throw new UnauthorizedException("Only the sender can unsend this message");
        }
        LocalDateTime sentAt = message.getSentAt() == null ? LocalDateTime.now() : message.getSentAt();
        if (sentAt.plus(UNSEND_WINDOW).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Messages can only be unsent within " + UNSEND_WINDOW.toMinutes() + " minutes");
        }
        message.setDeletedForEveryone(true);
        message.setContent("This message was unsent");
        message.setVoiceUrl(null);
        message.setAttachmentUrl(null);
        message.setDeletedAt(LocalDateTime.now());
        return toMap(messageRepository.save(message));
    }

    /** Unread count */
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return messageRepository.countUnread(userId);
    }

    private void assertConnected(Long senderId, Long receiverId) {
        boolean connected =
                connectionRepository.findByRequesterId(senderId).stream()
                        .anyMatch(c -> c.getReceiver().getId().equals(receiverId)
                                && c.getStatus() == NetworkingConnection.ConnectionStatus.ACCEPTED)
                || connectionRepository.findByReceiverId(senderId).stream()
                        .anyMatch(c -> c.getRequester().getId().equals(receiverId)
                                && c.getStatus() == NetworkingConnection.ConnectionStatus.ACCEPTED);
        if (!connected) throw new UnauthorizedException("You can only chat with accepted connections");
    }

    private User loadUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private DirectMessage loadMessageForParticipant(Long messageId, Long userId) {
        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (!message.getSender().getId().equals(userId) && !message.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedException("You cannot access this conversation");
        }
        return message;
    }

    private boolean visibleTo(DirectMessage message, Long userId) {
        if (message.getSender().getId().equals(userId)) return !message.isDeletedForSender();
        if (message.getReceiver().getId().equals(userId)) return !message.isDeletedForReceiver();
        return false;
    }

    private Map<String, Object> toMap(DirectMessage m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",                     m.getId());
        map.put("senderId",               m.getSender().getId());
        map.put("senderName",             m.getSender().getName());
        map.put("receiverId",             m.getReceiver().getId());
        map.put("content",                m.getContent());
        map.put("messageType",            m.getMessageType() != null ? m.getMessageType() : "TEXT");
        map.put("voiceUrl",               m.getVoiceUrl());
        map.put("voiceDurationSeconds",   m.getVoiceDurationSeconds());
        map.put("attachmentUrl",          m.getAttachmentUrl());
        map.put("read",                   m.isRead());
        map.put("edited",                 m.isEdited());
        map.put("deletedForEveryone",     m.isDeletedForEveryone());
        map.put("deleted",                m.isDeletedForEveryone());
        map.put("sentAt",                 m.getSentAt());
        return map;
    }
}
