package com.eventbooking.repository;

import com.eventbooking.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByBookingId(Long bookingId);

    Page<Payment> findByBookingUserId(Long userId, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.booking.event.organizer.id = :organizerId
          AND p.paymentStatus = :status
    """)
    BigDecimal sumRevenueByOrganizer(@Param("organizerId") Long organizerId,
                                    @Param("status") Payment.PaymentStatus status);

    default BigDecimal sumRevenueByOrganizer(Long organizerId) {
        BigDecimal result = sumRevenueByOrganizer(organizerId, Payment.PaymentStatus.SUCCESSFUL);
        return result != null ? result : BigDecimal.ZERO;
    }
}
