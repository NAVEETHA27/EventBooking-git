package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_history", indexes = {
        @Index(name = "idx_cert_history_certificate", columnList = "certificate_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CertificateHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_pk")
    private Certificate certificate;

    @Column(name = "certificate_id", length = 50)
    private String certificateId;

    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_role", length = 30)
    private String actorRole;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
