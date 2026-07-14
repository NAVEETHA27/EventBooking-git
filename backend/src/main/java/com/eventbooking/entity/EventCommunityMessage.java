package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_community_messages", indexes = {
        @Index(name = "idx_event_community_event", columnList = "event_id,sent_at"),
        @Index(name = "idx_event_community_sent_at", columnList = "sent_at"),
        @Index(name = "idx_event_community_sender", columnList = "sender_id,sender_role")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventCommunityMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_role", nullable = false, length = 30)
    private String senderRole;

    @Column(name = "sender_name", nullable = false, length = 160)
    private String senderName;

    @Column(name = "message_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "pinned", nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @Column(name = "announcement", nullable = false)
    @Builder.Default
    private Boolean announcement = false;

    @Column(name = "edited", nullable = false)
    @Builder.Default
    private Boolean edited = false;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "moderation_status", nullable = false, length = 30)
    @Builder.Default
    private String moderationStatus = "CLEAN";

    @CreatedDate
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum MessageType {
        TEXT, IMAGE, FILE
    }

    @PrePersist
    void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
        if (content == null) {
            content = message == null ? "" : message;
        }
    }

    @PreUpdate
    void onUpdate() {
        content = message == null ? "" : message;
    }
}
