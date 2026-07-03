package com.eventbooking.payment;

import com.eventbooking.dto.response.RefundResponse;
import com.eventbooking.entity.Payment;
import com.eventbooking.entity.Refund;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.RefundRepository;
import com.eventbooking.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<RefundResponse> byPayment(Long paymentId) {
        return refundRepository.findByPaymentId(paymentId).stream().map(this::toResponse).toList();
    }

    /** All refunds for the logged-in user */
    @Transactional(readOnly = true)
    public List<RefundResponse> myRefunds(Long userId) {
        return refundRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    public RefundResponse toResponse(Refund refund) {
        LocalDate expected = refund.getRefundDate() == null
                ? LocalDate.now().plusDays(7)
                : refund.getRefundDate().toLocalDate().plusDays(7);
        return RefundResponse.builder()
                .id(refund.getId())
                .amount(refund.getRefundAmount())
                .status(refund.getRefundStatus())
                .expectedRefundDate(expected)
                .acknowledgement("Refunds generally take 3-7 working days depending on your payment provider.")
                .reason(refund.getReason())
                .build();
    }

    @Transactional
    public RefundResponse updateStatus(Long refundId, Refund.RefundStatus status, Long adminId, String note) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        refund.setRefundStatus(status);
        if (note != null && !note.isBlank()) {
            refund.setReason(refund.getReason() == null ? note : refund.getReason() + " | " + note);
        }
        Refund saved = refundRepository.save(refund);
        updatePaymentRefundStatus(saved.getPayment());
        auditService.record(adminId, "ADMIN", "REFUND_STATUS_UPDATED", "REFUND",
                String.valueOf(refundId), "Refund status changed to " + status);
        return toResponse(saved);
    }

    private void updatePaymentRefundStatus(Payment payment) {
        if (payment == null) return;
        BigDecimal completedRefunds = refundRepository.findByPaymentId(payment.getId()).stream()
                .filter(r -> r.getRefundStatus() == Refund.RefundStatus.COMPLETED)
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (completedRefunds.compareTo(BigDecimal.ZERO) <= 0) return;

        payment.setPaymentStatus(completedRefunds.compareTo(payment.getAmount()) >= 0
                ? Payment.PaymentStatus.REFUNDED
                : Payment.PaymentStatus.PARTIALLY_REFUNDED);
    }
}
