package com.eventbooking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq", indexes = {
        @Index(name = "idx_faq_category", columnList = "category"),
        @Index(name = "idx_faq_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Faq {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String category;

    @NotBlank @Size(max = 240)
    @Column(nullable = false, length = 240)
    private String question;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
