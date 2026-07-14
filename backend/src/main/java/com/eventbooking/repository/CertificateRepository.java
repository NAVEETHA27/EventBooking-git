package com.eventbooking.repository;

import com.eventbooking.entity.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCertificateId(String certificateId);

    List<Certificate> findByUserId(Long userId);

    Page<Certificate> findByUserId(Long userId, Pageable pageable);

    Page<Certificate> findByUserIdAndEventNameContainingIgnoreCase(Long userId, String eventName, Pageable pageable);

    List<Certificate> findByEventIdAndStatus(Long eventId, Certificate.CertificateStatus status);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    Optional<Certificate> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT c FROM Certificate c WHERE c.event.id = :eventId")
    List<Certificate> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(c) FROM Certificate c WHERE c.user.id = :userId AND c.status = :status")
    long countGeneratedByUserId(@Param("userId") Long userId, @Param("status") Certificate.CertificateStatus status);

    default long countGeneratedByUserId(Long userId) {
        return countGeneratedByUserId(userId, Certificate.CertificateStatus.GENERATED);
    }

    @Query("SELECT c FROM Certificate c WHERE c.status = :status AND c.event.endDate <= CURRENT_DATE")
    List<Certificate> findPendingReadyForGeneration(@Param("status") Certificate.CertificateStatus status);

    default List<Certificate> findPendingReadyForGeneration() {
        return findPendingReadyForGeneration(Certificate.CertificateStatus.PENDING);
    }

    @Query("""
        SELECT c FROM Certificate c
        WHERE (:q IS NULL OR LOWER(c.certificateId) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(c.recipientName) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(c.eventName) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<Certificate> searchAll(@Param("q") String q, Pageable pageable);

    long countByStatus(Certificate.CertificateStatus status);

    long countByEmailSentFalse();
}
