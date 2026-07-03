package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_requests", indexes = {
        @Index(name = "idx_approval_event", columnList = "event_id"),
        @Index(name = "idx_approval_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "review_note", length = 800)
    private String reviewNote;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @CreatedDate
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @LastModifiedDate
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED, MODIFICATION_REQUESTED
    }
}
