package com.eventbooking.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentStatusUpdateRequest {
    @Size(max = 40)
    private String paymentMethod;
    @Size(max = 120)
    private String gatewayTransactionId;
    @Size(max = 300)
    private String failureReason;
}
