package com.eventbooking.repository;

import com.eventbooking.entity.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    Page<ApprovalRequest> findByStatus(ApprovalRequest.ApprovalStatus status, Pageable pageable);
    Optional<ApprovalRequest> findFirstByEventIdOrderByRequestedAtDesc(Long eventId);
}
