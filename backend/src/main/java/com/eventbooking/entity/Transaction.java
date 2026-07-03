package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a financial transaction linked to a booking.
 * Each transaction records a payment attempt against an Event by a User.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_id",     columnList = "txn_id",         unique = true),
        @Index(name = "idx_txn_user",   columnList = "user_id"),
        @Index(name = "idx_txn_event",  columnList = "event_id"),
        @Index(name = "idx_txn_status", columnList = "payment_status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique transaction identifier, format: TXN-<UUID> */
    @Column(name = "txn_id", nullable = false, unique = true, length = 60)
    private String txnId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum PaymentStatus {
        PENDING,
        SUCCESS,
        FAILED
    }
}
