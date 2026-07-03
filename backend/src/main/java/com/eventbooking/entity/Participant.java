package com.eventbooking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "participants", indexes = {
        @Index(name = "idx_participant_booking", columnList = "booking_id"),
        @Index(name = "idx_participant_event_email", columnList = "event_id,email")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_participant_event_email", columnNames = {"event_id", "email"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Email
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String email;

    @Size(max = 150)
    @Column(length = 150)
    private String department;

    @Size(max = 200)
    @Column(length = 200)
    private String college;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
