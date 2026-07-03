package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a payment refund.
 */
@Entity
@Table(name = "refunds", indexes = {
        @Index(name = "idx_refund_payment", columnList = "payment_id"),
        @Index(name = "idx_refund_status",  columnList = "refund_status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 30)
    @Builder.Default
    private RefundStatus refundStatus = RefundStatus.INITIATED;

    @Column(name = "reason", length = 400)
    private String reason;

    @Column(name = "refund_reference", length = 100)
    private String refundReference;

    @CreatedDate
    @Column(name = "refund_date", updatable = false)
    private LocalDateTime refundDate;

    public enum RefundStatus {
        INITIATED, REFUND_REQUESTED, UNDER_VERIFICATION, APPROVED, PROCESSING, COMPLETED, REJECTED, FAILED
    }
}
