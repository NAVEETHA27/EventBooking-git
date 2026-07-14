package com.eventbooking.payment;

import com.eventbooking.dto.request.PaymentStatusUpdateRequest;
import com.eventbooking.dto.request.RazorpayVerifyRequest;
import com.eventbooking.dto.response.BookingResponse;
import com.eventbooking.dto.response.PaymentResponse;
import com.eventbooking.dto.response.RazorpayOrderResponse;
import com.eventbooking.exception.BookingException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Payment;
import com.eventbooking.notification.NotificationService;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.PaymentRepository;
import com.eventbooking.service.AuditService;
import com.eventbooking.service.BookingService;
import com.eventbooking.service.EmailService;
import com.eventbooking.util.QrCodeGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;
    private final QrCodeGenerator qrCodeGenerator;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;
    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;
    @Value("${razorpay.currency:INR}")
    private String razorpayCurrency;

    @Transactional(readOnly = true)
    public Page<PaymentResponse> history(Long userId, int page, int size) {
        return paymentRepository.findByBookingUserId(userId, PageRequest.of(page, size, Sort.by("paidAt").descending()))
                .map(this::toResponse);
    }

    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(Long bookingId, Long userId) {
        Booking booking = getOwnedBooking(bookingId, userId);
        Payment payment = getPayment(bookingId);

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED
                || booking.getBookingStatus() == Booking.BookingStatus.EXPIRED) {
            throw new BookingException("This booking is " + booking.getBookingStatus().name().toLowerCase()
                    + ". Please create a new booking before paying.");
        }

        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            markSuccessful(bookingId, userId, PaymentStatusUpdateRequest.builder()
                    .paymentMethod("FREE")
                    .gatewayTransactionId("FREE-" + booking.getTicketId())
                    .build());
            return RazorpayOrderResponse.builder()
                    .bookingId(bookingId)
                    .displayAmount(BigDecimal.ZERO)
                    .currency(razorpayCurrency)
                    .eventName(booking.getEvent().getEventName())
                    .customerName(booking.getUser().getName())
                    .customerEmail(booking.getUser().getEmail())
                    .build();
        }

        ensureRazorpayConfigured();
        if (payment.getPaymentStatus() == Payment.PaymentStatus.SUCCESSFUL) {
            throw new BookingException("Payment is already successful");
        }
        if (payment.getPaymentStatus() == Payment.PaymentStatus.REFUNDED
                || payment.getPaymentStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BookingException("This booking payment was refunded and cannot be paid again.");
        }

        int amountInPaise = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();

        try {
            String receipt = "booking_" + bookingId + "_" + payment.getId();
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "amount", amountInPaise,
                    "currency", razorpayCurrency,
                    "receipt", receipt,
                    "payment_capture", 1
            ));

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                            (razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = response.statusCode() == 401 || response.statusCode() == 403
                        ? "Razorpay rejected the configured test credentials. Check RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET."
                        : "Razorpay order creation failed. Please try again.";
                String details = extractGatewayError(response.body());
                throw new BookingException(details == null ? message : message + " " + details);
            }

            JsonNode body = objectMapper.readTree(response.body());
            payment.setPaymentStatus(Payment.PaymentStatus.PROCESSING);
            payment.setPaymentMethod("RAZORPAY");
            payment.setGatewayOrderId(body.path("id").asText());
            payment.setFailureReason(null);
            paymentRepository.save(payment);
            auditService.record(userId, "USER", "RAZORPAY_ORDER_CREATED", "PAYMENT",
                    String.valueOf(payment.getId()), "Razorpay order created for booking " + bookingId);

            return RazorpayOrderResponse.builder()
                    .keyId(razorpayKeyId)
                    .orderId(payment.getGatewayOrderId())
                    .currency(razorpayCurrency)
                    .amount(amountInPaise)
                    .displayAmount(payment.getAmount())
                    .bookingId(bookingId)
                    .eventName(booking.getEvent().getEventName())
                    .customerName(booking.getUser().getName())
                    .customerEmail(booking.getUser().getEmail())
                    .build();
        } catch (BookingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BookingException("Could not create Razorpay order. " + safeMessage(ex));
        }
    }

    @Transactional
    public BookingResponse verifyRazorpayPayment(Long bookingId, Long userId, RazorpayVerifyRequest request) {
        ensureRazorpayConfigured();
        Payment payment = getPayment(bookingId);
        getOwnedBooking(bookingId, userId);
        if (!StringUtils.hasText(payment.getGatewayOrderId())
                || !payment.getGatewayOrderId().equals(request.getRazorpayOrderId())) {
            recordFailedVerification(payment, userId, "Razorpay order mismatch");
            throw new BookingException("Razorpay order mismatch");
        }
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        if (!hmacSha256(payload, razorpayKeySecret).equals(request.getRazorpaySignature())) {
            recordFailedVerification(payment, userId, "Razorpay signature verification failed");
            throw new BookingException("Razorpay signature verification failed");
        }
        return markSuccessful(bookingId, userId, PaymentStatusUpdateRequest.builder()
                .paymentMethod(StringUtils.hasText(request.getPaymentMethod()) ? request.getPaymentMethod() : "RAZORPAY")
                .gatewayTransactionId(request.getRazorpayPaymentId())
                .build());
    }

    @Transactional
    public PaymentResponse markProcessing(Long bookingId, Long userId, PaymentStatusUpdateRequest request) {
        Booking booking = getOwnedBooking(bookingId, userId);
        Payment payment = getPayment(bookingId);
        if (payment.getPaymentStatus() != Payment.PaymentStatus.PENDING
                && payment.getPaymentStatus() != Payment.PaymentStatus.FAILED
                && payment.getPaymentStatus() != Payment.PaymentStatus.PROCESSING) {
            throw new BookingException("Payment cannot be moved to processing from " + payment.getPaymentStatus());
        }
        payment.setPaymentStatus(Payment.PaymentStatus.PROCESSING);
        payment.setFailureReason(null);
        applyGatewayFields(payment, request);
        Payment saved = paymentRepository.save(payment);
        auditService.record(userId, "USER", "PAYMENT_PROCESSING", "PAYMENT",
                String.valueOf(saved.getId()), "Payment processing for booking " + booking.getId());
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse markSuccessful(Long bookingId, Long userId, PaymentStatusUpdateRequest request) {
        Booking booking = getOwnedBooking(bookingId, userId);
        Payment payment = getPayment(bookingId);
        if (payment.getPaymentStatus() == Payment.PaymentStatus.REFUNDED
                || payment.getPaymentStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BookingException("Payment cannot be marked successful from " + payment.getPaymentStatus());
        }

        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        // Generate QR only after payment confirmed
        try {
            booking.setQrCodePath(qrCodeGenerator.generate(booking.getTicketId()));
        } catch (Exception ex) {
            log.warn("QR generation failed for ticket {}: {}", booking.getTicketId(), ex.getMessage());
        }
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESSFUL);
        applyGatewayFields(payment, request);
        if (!StringUtils.hasText(payment.getGatewayReference())) {
            payment.setGatewayReference("GW-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        }
        paymentRepository.save(payment);
        notificationService.sendNotification(userId, "USER", "PAYMENT_SUCCESSFUL", "Payment successful",
                "Your payment is complete and ticket is confirmed.", "/bookings/" + bookingId);

        // Extract all data needed for email BEFORE saving/closing the transaction,
        // using plain strings — never pass live JPA entities to @Async methods.
        // Passing entities causes Hibernate session corruption (IllegalStateException:
        // Illegal pop() with non-matching JdbcValuesSourceProcessingState).
        String userEmail   = booking.getUser().getEmail();
        String userName    = booking.getUser().getName();
        String ticketId    = booking.getTicketId();
        int    quantity    = booking.getQuantity();
        double totalAmt    = booking.getTotalAmount().doubleValue();
        String eventName   = booking.getEvent().getEventName();
        String eventDate   = booking.getEvent().getEventDate() != null ? booking.getEvent().getEventDate().toString() : "";
        String eventTime   = booking.getEvent().getEventTime() != null ? booking.getEvent().getEventTime().toString() : "";
        String venue       = booking.getEvent().getVenueName() != null
                             ? booking.getEvent().getVenueName()
                             : (booking.getEvent().getLocation() != null ? booking.getEvent().getLocation() : "");

        auditService.record(userId, "USER", "PAYMENT_SUCCESSFUL", "PAYMENT",
                String.valueOf(payment.getId()), "Payment completed for booking " + bookingId);
        Booking saved = bookingRepository.save(booking);

        // Send email after transaction data is captured — safe to call async now
        emailService.sendBookingConfirmationDetached(userEmail, userName, ticketId, quantity, totalAmt,
                eventName, eventDate, eventTime, venue);

        return bookingService.toResponse(saved);
    }

    @Transactional
    public PaymentResponse markFailed(Long bookingId, Long userId, PaymentStatusUpdateRequest request) {
        getOwnedBooking(bookingId, userId);
        Payment payment = getPayment(bookingId);
        if (payment.getPaymentStatus() == Payment.PaymentStatus.SUCCESSFUL
                || payment.getPaymentStatus() == Payment.PaymentStatus.REFUNDED
                || payment.getPaymentStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BookingException("Payment cannot be marked failed from " + payment.getPaymentStatus());
        }
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        applyGatewayFields(payment, request);
        payment.setFailureReason(request != null && StringUtils.hasText(request.getFailureReason())
                ? request.getFailureReason()
                : "Payment failed");
        Payment saved = paymentRepository.save(payment);
        auditService.record(userId, "USER", "PAYMENT_FAILED", "PAYMENT",
                String.valueOf(saved.getId()), saved.getFailureReason());
        return toResponse(saved);
    }

    public PaymentResponse toResponse(Payment p) {
        String status = p.getPaymentStatus() == null ? "" : p.getPaymentStatus().name();
        String label = switch (p.getPaymentStatus()) {
            case PENDING -> "Pending";
            case PROCESSING -> "Processing";
            case SUCCESSFUL -> "Successful";
            case FAILED -> "Failed";
            case REFUNDED -> "Refunded";
            case PARTIALLY_REFUNDED -> "Partially Refunded";
        };
        return PaymentResponse.builder()
                .paymentId(p.getId())
                .gatewayTransactionId(p.getGatewayReference() != null ? p.getGatewayReference() : p.getTransactionId())
                .gatewayOrderId(p.getGatewayOrderId())
                .dateTime(p.getPaidAt())
                .date(p.getPaidAt() != null ? p.getPaidAt().toLocalDate().toString() : null)
                .time(p.getPaidAt() != null ? p.getPaidAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : null)
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getPaymentStatus())
                .statusLabel(label.isBlank() ? status : label)
                .bookingId(p.getBooking() != null ? p.getBooking().getId() : null)
                .eventName(p.getBooking() != null && p.getBooking().getEvent() != null ? p.getBooking().getEvent().getEventName() : null)
                .ticketId(p.getBooking() != null ? p.getBooking().getTicketId() : null)
                .build();
    }

    private Booking getOwnedBooking(Long bookingId, Long userId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private Payment getPayment(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private void applyGatewayFields(Payment payment, PaymentStatusUpdateRequest request) {
        if (request == null) return;
        if (StringUtils.hasText(request.getPaymentMethod())) {
            payment.setPaymentMethod(StringUtils.trimWhitespace(request.getPaymentMethod()));
        }
        if (StringUtils.hasText(request.getGatewayTransactionId())) {
            payment.setGatewayReference(StringUtils.trimWhitespace(request.getGatewayTransactionId()));
        }
    }

    private String extractGatewayError(String body) {
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            String description = error.path("description").asText(null);
            if (StringUtils.hasText(description)) {
                return description;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return StringUtils.hasText(message) ? message : "Please check the payment configuration and try again.";
    }

    private void ensureRazorpayConfigured() {
        if (!StringUtils.hasText(razorpayKeyId)
                || razorpayKeyId.contains("REPLACE_WITH")
                || !razorpayKeyId.startsWith("rzp_test_")
                || !StringUtils.hasText(razorpayKeySecret)
                || razorpayKeySecret.contains("REPLACE_WITH")) {
            throw new BookingException(
                "Razorpay Test Mode is not configured. " +
                "Set RAZORPAY_KEY_ID to an rzp_test_* key and set RAZORPAY_KEY_SECRET before starting.");
        }
    }

    private void recordFailedVerification(Payment payment, Long userId, String reason) {
        if (payment.getPaymentStatus() == Payment.PaymentStatus.SUCCESSFUL
                || payment.getPaymentStatus() == Payment.PaymentStatus.REFUNDED
                || payment.getPaymentStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            return;
        }
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);
        auditService.record(userId, "USER", "RAZORPAY_VERIFICATION_FAILED", "PAYMENT",
                String.valueOf(payment.getId()), reason);
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BookingException("Could not verify Razorpay signature");
        }
    }
}
