package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_queue", indexes = {
        @Index(name = "idx_queue_event_ts", columnList = "event_id,request_timestamp"),
        @Index(name = "idx_queue_status", columnList = "booking_status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingQueueEntry {
    @Id
    @Column(name = "request_id", length = 60)
    private String requestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "request_timestamp", nullable = false)
    private LocalDateTime requestTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 30)
    @Builder.Default
    private QueueStatus bookingStatus = QueueStatus.RECEIVED;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "message", length = 300)
    private String message;

    public enum QueueStatus {
        RECEIVED, PROCESSING, PAYMENT_PENDING, BOOKING_SUCCESSFUL, PAYMENT_FAILED, TICKETS_SOLD_OUT, EXPIRED, FAILED
    }
}
