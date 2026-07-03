package com.eventbooking.dto.request;

import com.eventbooking.entity.Refund;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundStatusUpdateRequest {
    @NotNull
    private Refund.RefundStatus status;
    private String note;
}
