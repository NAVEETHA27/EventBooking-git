package com.eventbooking.dto.response;

import com.eventbooking.entity.Refund;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundResponse {
    private Long id;
    private BigDecimal amount;
    private Refund.RefundStatus status;
    private LocalDate expectedRefundDate;
    private String acknowledgement;
    private String reason;
}
