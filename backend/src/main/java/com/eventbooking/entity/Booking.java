package com.eventbooking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a ticket booking made by a user for an event.
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_ticket",   columnList = "ticket_id",   unique = true),
        @Index(name = "idx_booking_user",     columnList = "user_id"),
        @Index(name = "idx_booking_event",    columnList = "event_id"),
        @Index(name = "idx_booking_status",   columnList = "booking_status"),
        @Index(name = "idx_booking_ticket_status", columnList = "ticket_status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false, unique = true, length = 50)
    private String ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 30)
    @Builder.Default
    private BookingStatus bookingStatus = BookingStatus.CONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", nullable = false, length = 30)
    @Builder.Default
    private TicketStatus ticketStatus = TicketStatus.ACTIVE;

    @Column(name = "qr_code_path")
    private String qrCodePath;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @CreatedDate
    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Participant> participants = new ArrayList<>();

    public enum BookingStatus {
        CONFIRMED, CANCELLED, PENDING, EXPIRED
    }

    public enum TicketStatus {
        ACTIVE, CANCELLED, EXPIRED
    }
}
