package com.eventbooking.service;

import com.eventbooking.entity.AuditLog;
import com.eventbooking.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    @Async
    public void record(Long actorId, String actorType, String action, String targetType, String targetId, String details) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .actorId(actorId)
                    .actorType(actorType)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .details(details)
                    .build());
        } catch (Exception ex) {
            log.warn("Audit log write failed for action {}: {}", action, ex.getMessage());
        }
    }
}
