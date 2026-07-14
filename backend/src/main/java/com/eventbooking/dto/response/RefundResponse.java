package com.eventbooking.dto.response;

import com.eventbooking.entity.Refund;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundResponse {
    private Long   id;
    private String refundId;        // refund_reference (e.g. REF-XXXXXXX)
    private BigDecimal amount;
    private Refund.RefundStatus status;
    private String eventName;       // for display in RefundTracking UI
    private LocalDateTime requestedAt;
    private LocalDate expectedRefundDate;
    private String acknowledgement;
    private String reason;
    private String transactionRef;  // original payment transaction ID
}
