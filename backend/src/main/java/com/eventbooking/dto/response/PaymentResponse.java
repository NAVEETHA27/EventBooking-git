package com.eventbooking.dto.response;

import com.eventbooking.entity.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponse {
    private Long paymentId;
    private String gatewayTransactionId;
    private String gatewayOrderId;
    private LocalDateTime dateTime;
    private String date;
    private String time;
    private BigDecimal amount;
    private String paymentMethod;
    private Payment.PaymentStatus status;
    private String statusLabel;
    private Long bookingId;
    private String eventName;
    private String ticketId;
}
