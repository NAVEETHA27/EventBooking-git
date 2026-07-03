package com.eventbooking.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RazorpayOrderResponse {
    private String keyId;
    private String orderId;
    private String currency;
    private Integer amount;
    private BigDecimal displayAmount;
    private Long bookingId;
    private String eventName;
    private String customerName;
    private String customerEmail;
}
