package com.eventbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentToolResponse {
    private String toolName;
    private String status;
    private boolean requiresConfirmation;
    private String message;
    private LocalDateTime timestamp;

    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
}
