package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * AI-suggested networking connection between two users.
 */
@Entity
@Table(name = "networking_connections", indexes = {
        @Index(name = "idx_net_requester", columnList = "requester_id"),
        @Index(name = "idx_net_receiver", columnList = "receiver_id"),
        @Index(name = "idx_net_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NetworkingConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.PENDING;

    /** AI reason for suggestion */
    @Column(name = "match_reason", length = 400)
    private String matchReason;

    @Column(name = "match_score")
    private Double matchScore;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ConnectionStatus {
        PENDING, ACCEPTED, REJECTED
    }
}
