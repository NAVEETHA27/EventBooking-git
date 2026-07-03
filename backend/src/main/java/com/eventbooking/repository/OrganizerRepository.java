package com.eventbooking.repository;

import com.eventbooking.entity.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, Long> {

    Optional<Organizer> findByEmail(String email);
    Optional<Organizer> findByOrganizerCode(String organizerCode);

    boolean existsByEmail(String email);

    Optional<Organizer> findByVerificationToken(String token);

    Optional<Organizer> findByResetPasswordToken(String token);
}
