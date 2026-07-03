package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actor_id,actor_type"),
        @Index(name = "idx_audit_action", columnList = "action")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_id")
    private Long actorId;
    @Column(name = "actor_type", length = 30)
    private String actorType;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(name = "target_type", length = 60)
    private String targetType;
    @Column(name = "target_id", length = 80)
    private String targetId;
    @Column(columnDefinition = "TEXT")
    private String details;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
