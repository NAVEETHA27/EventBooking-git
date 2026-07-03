package com.eventbooking.service;

import com.eventbooking.entity.ApprovalRequest;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.Payment;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository             userRepository;
    private final OrganizerRepository        organizerRepository;
    private final EventRepository            eventRepository;
    private final PaymentRepository          paymentRepository;
    private final RefundRepository           refundRepository;
    private final ApprovalRequestRepository  approvalRequestRepository;
    private final AuditLogRepository         auditLogRepository;

    public Map<String, Object> stats() {
        BigDecimal revenue = paymentRepository.findAll().stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.SUCCESSFUL)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "totalUsers",       userRepository.count(),
                "totalOrganizers",  organizerRepository.count(),
                "totalEvents",      eventRepository.count(),
                "pendingApprovals", approvalRequestRepository
                        .findByStatus(ApprovalRequest.ApprovalStatus.PENDING,
                                PageRequest.of(0, 1)).getTotalElements(),
                "totalRevenue",     revenue,
                "refundRequests",   refundRepository.count(),
                "activeEvents",     eventRepository.findByStatus(Event.EventStatus.PUBLISHED).size(),
                "cancelledEvents",  eventRepository.findByStatus(Event.EventStatus.CANCELLED).size()
        );
    }

    public Page<ApprovalRequest> approvals(ApprovalRequest.ApprovalStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestedAt").descending());
        return status == null
                ? approvalRequestRepository.findAll(pageable)
                : approvalRequestRepository.findByStatus(status, pageable);
    }

    public Page<?> users(Pageable pageable)      { return userRepository.findAll(pageable); }
    public Page<?> organizers(Pageable pageable) { return organizerRepository.findAll(pageable); }
    public Page<?> events(Pageable pageable)     { return eventRepository.findAll(pageable); }
    public Page<?> payments(Pageable pageable)   { return paymentRepository.findAll(pageable); }
    public Page<?> refunds(Pageable pageable)    { return refundRepository.findAll(pageable); }
    public Page<?> auditLogs(Pageable pageable)  { return auditLogRepository.findAll(pageable); }
}
