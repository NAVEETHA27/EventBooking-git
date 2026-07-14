package com.eventbooking.repository;

import com.eventbooking.entity.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    Page<ApprovalRequest> findByStatus(ApprovalRequest.ApprovalStatus status, Pageable pageable);
    Optional<ApprovalRequest> findFirstByEventIdOrderByRequestedAtDesc(Long eventId);

    /** Find all PENDING approvals whose request was created before the given cutoff time. */
    @Query("SELECT a FROM ApprovalRequest a WHERE a.status = 'PENDING' AND a.requestedAt <= :cutoff")
    List<ApprovalRequest> findPendingOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** Fetch approval with event and organizer eagerly loaded — used by scheduler. */
    @Query("SELECT a FROM ApprovalRequest a JOIN FETCH a.event e JOIN FETCH e.organizer WHERE a.id = :id")
    Optional<ApprovalRequest> findByIdWithEventAndOrganizer(@Param("id") Long id);
}
