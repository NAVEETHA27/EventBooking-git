package com.eventbooking.notification;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.mongo.Notification;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.mongo.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles in-app notifications (SSE stream) and persists them to MongoDB.
 *
 * MongoDB dependency is OPTIONAL:
 *   - When MongoDB is available: notifications are persisted and queryable.
 *   - When MongoDB is unavailable: SSE streaming still works; persist calls
 *     are silently skipped with a WARN log. No startup failure occurs.
 */
@Service
@Slf4j
public class NotificationService {

    /** Injected as Optional — null when mongodb.enabled=false or Mongo is down. */
    private final Optional<NotificationRepository> notificationRepository;
    private final BookingRepository bookingRepository;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    @Autowired
    public NotificationService(
            @Autowired(required = false) NotificationRepository notificationRepository,
            BookingRepository bookingRepository) {
        this.notificationRepository = Optional.ofNullable(notificationRepository);
        this.bookingRepository      = bookingRepository;
        if (this.notificationRepository.isEmpty()) {
            log.warn("[NotificationService] MongoDB is unavailable — notifications will NOT be persisted. " +
                     "SSE streaming still works. Set mongodb.enabled=true to enable persistence.");
        }
    }

    // ── Send ─────────────────────────────────────────────────────────────

    @Async
    public void sendNotification(Long recipientId, String recipientType,
                                  String type, String title, String message, String actionUrl) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .recipientType(recipientType)
                .notificationType(type)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.ifPresentOrElse(
            repo -> {
                try {
                    Notification saved = repo.save(notification);
                    emit(recipientId, recipientType, saved);
                } catch (Exception ex) {
                    log.warn("[Notification] MongoDB save failed for recipient {}: {}",
                             recipientId, ex.getMessage());
                    // Still push via SSE even if persist failed
                    emit(recipientId, recipientType, notification);
                }
            },
            () -> {
                // MongoDB not available — push via SSE only
                emit(recipientId, recipientType, notification);
            }
        );
    }

    // ── Query ─────────────────────────────────────────────────────────────

    public Page<Notification> getNotifications(Long recipientId, String recipientType,
                                                int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        if (notificationRepository.isEmpty()) {
            return Page.empty(pageable);
        }
        try {
            return notificationRepository.get()
                    .findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(
                            recipientId, recipientType, pageable);
        } catch (Exception ex) {
            log.warn("[Notification] MongoDB read failed for recipient {}:{}; returning empty page: {}",
                    recipientType, recipientId, ex.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }

    public long getUnreadCount(Long recipientId, String recipientType) {
        if (notificationRepository.isEmpty()) {
            return 0L;
        }
        try {
            return notificationRepository.get()
                    .countByRecipientIdAndRecipientTypeAndReadFalse(recipientId, recipientType);
        } catch (Exception ex) {
            log.warn("[Notification] MongoDB unread-count failed for recipient {}:{}; returning 0: {}",
                    recipientType, recipientId, ex.getMessage());
            return 0L;
        }
    }

    public void markAllRead(Long recipientId, String recipientType) {
        if (notificationRepository.isEmpty()) {
            return;
        }
        try {
            Pageable batch = PageRequest.of(0, 500);
            notificationRepository.get()
                    .findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(recipientId, recipientType, batch)
                    .getContent()
                    .forEach(n -> {
                        n.setRead(true);
                        n.setReadAt(LocalDateTime.now());
                        notificationRepository.get().save(n);
                    });
        } catch (Exception ex) {
            log.warn("[Notification] MongoDB mark-all-read failed for recipient {}:{}: {}",
                    recipientType, recipientId, ex.getMessage());
        }
    }

    // ── SSE Stream ────────────────────────────────────────────────────────

    public SseEmitter stream(Long recipientId, String recipientType) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        String key = key(recipientId, recipientType);
        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(()    -> removeEmitter(key, emitter));
        emitter.onError(ex     -> removeEmitter(key, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            removeEmitter(key, emitter);
        }
        return emitter;
    }

    // ── Event helpers ─────────────────────────────────────────────────────

    @Async
    public void notifyEventUpdate(Event event) {
        bookingRepository.findByOrganizerEvents(event.getOrganizer().getId(), PageRequest.of(0, 500))
                .getContent().stream()
                .filter(b -> b.getEvent().getId().equals(event.getId())
                        && b.getBookingStatus() == Booking.BookingStatus.CONFIRMED)
                .forEach(b -> sendNotification(b.getUser().getId(), "USER",
                        "EVENT_UPDATED", "Event Updated",
                        event.getEventName() + " has been updated.", "/events/" + event.getId()));
    }

    @Async
    public void notifyEventCancellation(Event event) {
        bookingRepository.findByOrganizerEvents(event.getOrganizer().getId(), PageRequest.of(0, 500))
                .getContent().stream()
                .filter(b -> b.getEvent().getId().equals(event.getId())
                        && b.getBookingStatus() == Booking.BookingStatus.CONFIRMED)
                .forEach(b -> sendNotification(b.getUser().getId(), "USER",
                        "EVENT_CANCELLED", "Event Cancelled",
                        event.getEventName() + " has been cancelled. A refund will be processed.",
                        "/bookings"));
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private void emit(Long recipientId, String recipientType, Notification notification) {
        String key = key(recipientId, recipientType);
        List<SseEmitter> active = emitters.getOrDefault(key, new CopyOnWriteArrayList<>());
        for (SseEmitter emitter : active) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException ex) {
                removeEmitter(key, emitter);
            }
        }
    }

    private String key(Long recipientId, String recipientType) {
        return recipientType + ":" + recipientId;
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> active = emitters.get(key);
        if (active != null) active.remove(emitter);
    }
}
