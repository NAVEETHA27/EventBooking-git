package com.eventbooking.repository;

import com.eventbooking.entity.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {
    Optional<CertificateTemplate> findFirstByEventIdAndActiveTrueOrderByUploadedAtDesc(Long eventId);
}
