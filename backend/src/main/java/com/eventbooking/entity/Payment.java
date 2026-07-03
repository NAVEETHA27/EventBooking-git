package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a payment transaction for a booking.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_txn",     columnList = "transaction_id", unique = true),
        @Index(name = "idx_payment_booking", columnList = "booking_id"),
        @Index(name = "idx_payment_status",  columnList = "payment_status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 60)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_method", length = 40)
    private String paymentMethod;

    @Column(name = "gateway_reference", length = 120)
    private String gatewayReference;

    @Column(name = "gateway_order_id", length = 120)
    private String gatewayOrderId;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    @CreatedDate
    @Column(name = "paid_at", updatable = false)
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();

    public enum PaymentStatus {
        PENDING, PROCESSING, SUCCESSFUL, FAILED, REFUNDED, PARTIALLY_REFUNDED
    }
}
