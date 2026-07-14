package com.eventbooking.repository;

import com.eventbooking.entity.CertificateSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateSettingsRepository extends JpaRepository<CertificateSettings, Long> {
    Optional<CertificateSettings> findByEventId(Long eventId);
}
