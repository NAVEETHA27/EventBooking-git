package com.eventbooking.service;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.EventCommunityMessage;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.notification.NotificationService;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventCommunityMessageRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EventCommunityService {

    private static final List<String> ALLOWED_ATTACHMENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final EventCommunityMessageRepository messageRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;

    private final Map<Long, Boolean> manuallyClosedEvents = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> mutedMembers = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Map<String, Object> context(Long eventId, AuthPrincipal principal) {
        Event event = loadEvent(eventId);
        assertCanView(event, principal);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", eventId);
        map.put("groupName", event.getEventName() + " Community");
        map.put("canSend", canSend(event, principal));
        map.put("readOnlyReason", readOnlyReason(event, principal));
        map.put("moderator", isModerator(event, principal));
        map.put("pinnedMessages", pinned(eventId));
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> latest(Long eventId, int limit, AuthPrincipal principal) {
        Event event = loadEvent(eventId);
        assertCanView(event, principal);
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return messageRepository.findLatestByEvent(eventId, PageRequest.of(0, safeLimit))
                .stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::toMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> newerThan(Long eventId, Long afterId, AuthPrincipal principal) {
        Event event = loadEvent(eventId);
        assertCanView(event, principal);
        return messageRepository.findNewerThan(eventId, afterId == null ? 0L : afterId, PageRequest.of(0, 50))
                .stream()
                .map(this::toMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> olderThan(Long eventId, Long beforeId, AuthPrincipal principal) {
        Event event = loadEvent(eventId);
        assertCanView(event, principal);
        return messageRepository.findOlderThan(eventId, beforeId == null ? Long.MAX_VALUE : beforeId, PageRequest.of(0, 50))
                .stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::toMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> send(Long eventId, AuthPrincipal principal, String message, String messageType,
                                    String attachmentUrl, boolean announcement, boolean pin) {
        Event event = loadEvent(eventId);
        assertCanSend(event, principal);
        EventCommunityMessage saved = saveMessage(event, principal, message, messageType, attachmentUrl, announcement, pin);
        if (Boolean.TRUE.equals(saved.getPinned()) || Boolean.TRUE.equals(saved.getAnnouncement())) {
            notifyCommunity(event, saved, Boolean.TRUE.equals(saved.getPinned()) ? "COMMUNITY_PINNED" : "COMMUNITY_ANNOUNCEMENT");
        }
        return toMap(saved);
    }

    @Transactional
    public Map<String, Object> uploadAndSend(Long eventId, AuthPrincipal principal, String message, MultipartFile file) throws IOException {
        Event event = loadEvent(eventId);
        assertCanSend(event, principal);
        validateAttachment(file);
        String url = storageService.store("community/" + eventId, file, "chat");
        String contentType = file.getContentType();
        String type = contentType != null && contentType.startsWith("image/") ? "IMAGE" : "FILE";
        EventCommunityMessage saved = saveMessage(event, principal,
                StringUtils.hasText(message) ? message : file.getOriginalFilename(), type, url, false, false);
        return toMap(saved);
    }

    @Transactional
    public Map<String, Object> edit(Long eventId, Long messageId, AuthPrincipal principal, String message) {
        Event event = loadEvent(eventId);
        assertCanView(event, principal);
        EventCommunityMessage existing = loadMessage(eventId, messageId);
        if (!owns(existing, principal)) {
            throw new AccessDeniedException("Only the sender can edit this message");
        }
        validateText(message);
        existing.setMessage(message.trim());
        existing.setContent(message.trim());
        existing.setEdited(true);
        existing.setEditedAt(LocalDateTime.now());
        return toMap(messageRepository.save(existing));
    }

    @Transactional
    public Map<String, Object> delete(Long eventId, Long messageId, AuthPrincipal principal) {
        Event event = loadEvent(eventId);
        assertCanView(event, principal);
        EventCommunityMessage existing = loadMessage(eventId, messageId);
        if (!owns(existing, principal) && !isModerator(event, principal)) {
            throw new AccessDeniedException("Only the sender, organizer, or admin can delete this message");
        }
        existing.setDeleted(true);
        existing.setDeletedAt(LocalDateTime.now());
        existing.setMessage("This message was deleted");
        existing.setContent("This message was deleted");
        existing.setAttachmentUrl(null);
        return toMap(messageRepository.save(existing));
    }

    @Transactional
    public Map<String, Object> pin(Long eventId, Long messageId, AuthPrincipal principal, boolean pinned) {
        Event event = loadEvent(eventId);
        if (!isModerator(event, principal)) {
            throw new AccessDeniedException("Only organizers or admins can pin messages");
        }
        EventCommunityMessage existing = loadMessage(eventId, messageId);
        existing.setPinned(pinned);
        EventCommunityMessage saved = messageRepository.save(existing);
        if (pinned) {
            notifyCommunity(event, saved, "COMMUNITY_PINNED");
        }
        return toMap(saved);
    }

    public void closeChat(Long eventId, AuthPrincipal principal, boolean closed) {
        Event event = loadEvent(eventId);
        if (!isModerator(event, principal)) {
            throw new AccessDeniedException("Only organizers or admins can close chat");
        }
        manuallyClosedEvents.put(eventId, closed);
    }

    public void mute(Long eventId, Long senderId, String senderRole, int minutes, AuthPrincipal principal) {
        Event event = loadEvent(eventId);
        if (!isModerator(event, principal)) {
            throw new AccessDeniedException("Only organizers or admins can mute members");
        }
        mutedMembers.put(muteKey(eventId, senderId, senderRole), LocalDateTime.now().plusMinutes(Math.max(1, Math.min(minutes, 1440))));
    }

    private EventCommunityMessage saveMessage(Event event, AuthPrincipal principal, String message, String messageType,
                                              String attachmentUrl, boolean announcement, boolean pin) {
        validateTextOrAttachment(message, attachmentUrl);
        boolean moderator = isModerator(event, principal);
        EventCommunityMessage.MessageType type = parseMessageType(messageType, attachmentUrl);
        EventCommunityMessage entity = EventCommunityMessage.builder()
                .event(event)
                .senderId(principal.getId())
                .senderRole(normalizeRole(principal.getRole()))
                .senderName(displayName(principal))
                .message(message == null ? "" : message.trim())
                .content(message == null ? "" : message.trim())
                .messageType(type)
                .attachmentUrl(attachmentUrl)
                .announcement(moderator && announcement)
                .pinned(moderator && pin)
                .moderationStatus(moderate(message))
                .build();
        return messageRepository.save(entity);
    }

    private void assertCanView(Event event, AuthPrincipal principal) {
        if (principal == null || !canView(event, principal)) {
            throw new AccessDeniedException("Only booked attendees, the organizer, or admins can access this community");
        }
    }

    private void assertCanSend(Event event, AuthPrincipal principal) {
        assertCanView(event, principal);
        String reason = readOnlyReason(event, principal);
        if (reason != null) {
            throw new AccessDeniedException(reason);
        }
    }

    private boolean canView(Event event, AuthPrincipal principal) {
        if ("ADMIN".equalsIgnoreCase(principal.getRole())) return true;
        if (isOrganizer(event, principal)) return true;
        if ("USER".equalsIgnoreCase(principal.getRole())) {
            return bookingRepository.findConfirmedByUserAndEvent(principal.getId(), event.getId()).isPresent();
        }
        return false;
    }

    private boolean canSend(Event event, AuthPrincipal principal) {
        return canView(event, principal) && readOnlyReason(event, principal) == null;
    }

    private String readOnlyReason(Event event, AuthPrincipal principal) {
        if (Boolean.TRUE.equals(manuallyClosedEvents.get(event.getId()))) return "Community chat is closed by the organizer";
        LocalDateTime mutedUntil = mutedMembers.get(muteKey(event.getId(), principal.getId(), principal.getRole()));
        if (mutedUntil != null && mutedUntil.isAfter(LocalDateTime.now())) return "You are temporarily muted";
        if (isEnded(event)) return "Community chat is read-only after the event ends";
        // Moderators (organizer/admin) can always send
        if (isModerator(event, principal)) return null;
        // Users can send as long as they have a confirmed booking and the event is not yet ended
        if ("USER".equalsIgnoreCase(principal.getRole())) {
            boolean hasBooking = bookingRepository.findConfirmedByUserAndEvent(principal.getId(), event.getId()).isPresent();
            if (!hasBooking) return "Register for this event to join the community discussion";
            return null;
        }
        return "Community chat is not available";
    }

    private boolean isModerator(Event event, AuthPrincipal principal) {
        return principal != null && ("ADMIN".equalsIgnoreCase(principal.getRole()) || isOrganizer(event, principal));
    }

    private boolean isOrganizer(Event event, AuthPrincipal principal) {
        return principal != null
                && "ORGANIZER".equalsIgnoreCase(principal.getRole())
                && event.getOrganizer() != null
                && event.getOrganizer().getId().equals(principal.getId());
    }

    private boolean isRegistrationOpen(Event event) {
        if (event.getStatus() == Event.EventStatus.CANCELLED || event.getStatus() == Event.EventStatus.COMPLETED
                || event.getStatus() == Event.EventStatus.EXPIRED) return false;
        if (event.getAvailableSeats() <= 0) return false;
        LocalDateTime deadline = event.getRegistrationDeadline() == null
                ? startAt(event)
                : event.getRegistrationDeadline().atTime(LocalTime.MAX);
        return deadline == null || !LocalDateTime.now().isAfter(deadline);
    }

    private boolean isLive(Event event) {
        if (event.getStatus() == Event.EventStatus.LIVE || event.getStatus() == Event.EventStatus.ONGOING) return true;
        LocalDateTime start = startAt(event);
        LocalDateTime end = endAt(event);
        LocalDateTime now = LocalDateTime.now();
        return start != null && end != null && !now.isBefore(start) && !now.isAfter(end);
    }

    private boolean isEnded(Event event) {
        if (event.getStatus() == Event.EventStatus.COMPLETED || event.getStatus() == Event.EventStatus.EXPIRED) return true;
        LocalDateTime end = endAt(event);
        return end != null && LocalDateTime.now().isAfter(end);
    }

    private LocalDateTime startAt(Event event) {
        return event.getEventDate() == null ? null : event.getEventDate().atTime(event.getEventTime() == null ? LocalTime.MIN : event.getEventTime());
    }

    private LocalDateTime endAt(Event event) {
        if (event.getEventDate() == null && event.getEndDate() == null) return null;
        return (event.getEndDate() == null ? event.getEventDate() : event.getEndDate())
                .atTime(event.getEndTime() == null ? LocalTime.MAX : event.getEndTime());
    }

    private List<Map<String, Object>> pinned(Long eventId) {
        return messageRepository.findByEventIdAndPinnedTrueAndDeletedFalseOrderBySentAtDesc(eventId)
                .stream().map(this::toMap).toList();
    }

    private Event loadEvent(Long eventId) {
        return eventRepository.findByIdWithOrganizer(eventId)
                .or(() -> eventRepository.findById(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private EventCommunityMessage loadMessage(Long eventId, Long messageId) {
        EventCommunityMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (!message.getEvent().getId().equals(eventId)) {
            throw new ResourceNotFoundException("Message not found");
        }
        return message;
    }

    private void validateText(String content) {
        if (!StringUtils.hasText(content) || content.trim().length() > 2000) {
            throw new IllegalArgumentException("Message must be 1-2000 characters");
        }
    }

    private void validateTextOrAttachment(String content, String attachmentUrl) {
        if (!StringUtils.hasText(content) && !StringUtils.hasText(attachmentUrl)) {
            throw new IllegalArgumentException("Message or attachment is required");
        }
        if (content != null && content.trim().length() > 2000) {
            throw new IllegalArgumentException("Message must be 1-2000 characters");
        }
    }

    private void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Attachment is required");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_ATTACHMENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only images, PDF, and DOCX files are allowed");
        }
    }

    private EventCommunityMessage.MessageType parseMessageType(String value, String attachmentUrl) {
        if (!StringUtils.hasText(value)) return StringUtils.hasText(attachmentUrl) ? EventCommunityMessage.MessageType.FILE : EventCommunityMessage.MessageType.TEXT;
        try {
            return EventCommunityMessage.MessageType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return EventCommunityMessage.MessageType.TEXT;
        }
    }

    private String moderate(String content) {
        String text = content == null ? "" : content.toLowerCase(Locale.ROOT);
        if (text.contains("http://") || text.contains("bit.ly") || text.contains("free money")) return "REVIEW";
        return "CLEAN";
    }

    private boolean owns(EventCommunityMessage message, AuthPrincipal principal) {
        return principal != null
                && message.getSenderId().equals(principal.getId())
                && message.getSenderRole().equalsIgnoreCase(principal.getRole());
    }

    private String displayName(AuthPrincipal principal) {
        if ("ADMIN".equalsIgnoreCase(principal.getRole())) return "Admin";
        String local = principal.getEmail() == null ? "Event member" : principal.getEmail().split("@")[0];
        return StringUtils.capitalize(local.replace('.', ' ').replace('_', ' '));
    }

    private String normalizeRole(String role) {
        return StringUtils.hasText(role) ? role.trim().toUpperCase(Locale.ROOT) : "USER";
    }

    private String muteKey(Long eventId, Long senderId, String senderRole) {
        return eventId + ":" + senderId + ":" + normalizeRole(senderRole);
    }

    private void notifyCommunity(Event event, EventCommunityMessage message, String type) {
        String title = Boolean.TRUE.equals(message.getPinned()) ? "Pinned community update" : "Community announcement";
        String body = event.getEventName() + ": " + abbreviate(message.getMessage());
        bookingRepository.findConfirmedWithDetailsByEventId(event.getId()).stream()
                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CONFIRMED)
                .forEach(b -> notificationService.sendNotification(b.getUser().getId(), "USER", type, title, body, "/events/" + event.getId()));
        if (event.getOrganizer() != null) {
            notificationService.sendNotification(event.getOrganizer().getId(), "ORGANIZER", type, title, body, "/events/" + event.getId());
        }
    }

    private String abbreviate(String value) {
        if (value == null) return "";
        return value.length() > 120 ? value.substring(0, 117) + "..." : value;
    }

    private Map<String, Object> toMap(EventCommunityMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", message.getId());
        map.put("eventId", message.getEvent().getId());
        map.put("senderId", message.getSenderId());
        map.put("senderRole", message.getSenderRole());
        map.put("senderName", message.getSenderName());
        String body = StringUtils.hasText(message.getMessage()) ? message.getMessage() : message.getContent();
        map.put("message", body);
        map.put("content", body);
        map.put("messageType", message.getMessageType());
        map.put("attachmentUrl", message.getAttachmentUrl());
        map.put("sentAt", message.getSentAt());
        map.put("createdAt", message.getSentAt());
        map.put("edited", Boolean.TRUE.equals(message.getEdited()));
        map.put("deleted", Boolean.TRUE.equals(message.getDeleted()));
        map.put("pinned", Boolean.TRUE.equals(message.getPinned()));
        map.put("announcement", Boolean.TRUE.equals(message.getAnnouncement()));
        map.put("moderationStatus", message.getModerationStatus());
        return map;
    }
}
