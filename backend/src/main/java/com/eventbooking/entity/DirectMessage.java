package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * One-to-one direct message between two connected users.
 * Only ACCEPTED NetworkingConnection pairs can exchange messages.
 */
@Entity
@Table(name = "direct_messages", indexes = {
        @Index(name = "idx_dm_sender",   columnList = "sender_id"),
        @Index(name = "idx_dm_receiver", columnList = "receiver_id"),
        @Index(name = "idx_dm_conversation", columnList = "sender_id,receiver_id,sent_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** MESSAGE_TYPE: TEXT | VOICE | IMAGE | FILE */
    @Column(name = "message_type", length = 20)
    @Builder.Default
    private String messageType = "TEXT";

    /** URL to stored voice memo (audio/webm or audio/ogg) */
    @Column(name = "voice_url", length = 500)
    private String voiceUrl;

    /** Duration in seconds for voice messages */
    @Column(name = "voice_duration_seconds")
    private Integer voiceDurationSeconds;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    /** true once the receiver has fetched this message */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "edited", nullable = false)
    @Builder.Default
    private boolean edited = false;

    @Column(name = "deleted_for_sender", nullable = false)
    @Builder.Default
    private boolean deletedForSender = false;

    @Column(name = "deleted_for_receiver", nullable = false)
    @Builder.Default
    private boolean deletedForReceiver = false;

    @Column(name = "deleted_for_everyone", nullable = false)
    @Builder.Default
    private boolean deletedForEveryone = false;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;
}
