package com.eventbooking.repository;

import com.eventbooking.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByPaymentId(Long paymentId);

    /** All refunds for a specific user — used by /refunds/my endpoint */
    @Query("""
        SELECT r FROM Refund r
        WHERE r.payment.booking.user.id = :userId
        ORDER BY r.id DESC
    """)
    List<Refund> findByUserId(@Param("userId") Long userId);
}
