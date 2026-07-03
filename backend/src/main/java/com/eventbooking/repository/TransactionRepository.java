package com.eventbooking.repository;

import com.eventbooking.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserIdOrderByPaymentDateDesc(Long userId, Pageable pageable);

    Page<Transaction> findByEventOrganizerIdOrderByPaymentDateDesc(Long organizerId, Pageable pageable);
}
