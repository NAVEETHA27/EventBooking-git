package com.eventbooking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tutorial_videos", indexes = {
        @Index(name = "idx_video_category", columnList = "category"),
        @Index(name = "idx_video_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TutorialVideo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    @Column(length = 80)
    private String category;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
