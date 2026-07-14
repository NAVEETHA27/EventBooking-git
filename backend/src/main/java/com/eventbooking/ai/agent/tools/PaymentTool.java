package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.entity.Payment;
import com.eventbooking.entity.Refund;
import com.eventbooking.repository.PaymentRepository;
import com.eventbooking.repository.RefundRepository;
import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentTool implements AgentTool {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    @Override
    public String name() { return "paymentTool"; }

    @Override
    public String description() {
        return "Fetches user payment history, payment status, refund status, and transaction details.";
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (principal == null) {
            result.put("error", "Authentication required to view payments.");
            return result;
        }
        try {
            Long userId = principal.getId();

            List<Payment> payments = paymentRepository
                    .findByBookingUserId(userId, PageRequest.of(0, 10, Sort.by("paidAt").descending()))
                    .stream().collect(Collectors.toList());

            List<Map<String, Object>> paymentList = payments.stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("transactionId", p.getTransactionId());
                m.put("amount", p.getAmount());
                m.put("paymentStatus", p.getPaymentStatus());
                m.put("paymentMethod", p.getPaymentMethod());
                m.put("paidAt", p.getPaidAt());
                m.put("eventName", p.getBooking() != null && p.getBooking().getEvent() != null
                        ? p.getBooking().getEvent().getEventName() : "N/A");
                return m;
            }).collect(Collectors.toList());

            List<Refund> refunds = refundRepository.findByUserId(userId).stream()
                    .limit(5).collect(Collectors.toList());
            List<Map<String, Object>> refundList = refunds.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("refundReference", r.getRefundReference());
                m.put("refundAmount", r.getRefundAmount());
                m.put("refundStatus", r.getRefundStatus());
                m.put("reason", r.getReason());
                return m;
            }).collect(Collectors.toList());

            result.put("payments", paymentList);
            result.put("refunds", refundList);
            result.put("totalPayments", paymentList.size());
            long successful = payments.stream()
                    .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.SUCCESSFUL).count();
            result.put("successfulPayments", successful);
        } catch (Exception ex) {
            result.put("error", "Could not fetch payment data: " + ex.getMessage());
        }
        return result;
    }
}
