package com.eventbooking.repository;

import com.eventbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUserCode(String userCode);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByResetPasswordToken(String token);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
    void incrementFailedAttempts(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLocked = false WHERE u.id = :id")
    void resetFailedAttempts(Long id);
}
