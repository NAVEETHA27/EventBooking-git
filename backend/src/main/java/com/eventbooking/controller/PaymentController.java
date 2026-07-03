package com.eventbooking.controller;

import com.eventbooking.dto.request.PaymentStatusUpdateRequest;
import com.eventbooking.dto.request.RazorpayVerifyRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.dto.response.PaymentResponse;
import com.eventbooking.dto.response.RefundResponse;
import com.eventbooking.dto.response.RazorpayOrderResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.payment.PaymentService;
import com.eventbooking.payment.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final RefundService refundService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Page<PaymentResponse>> history(@AuthenticationPrincipal AuthPrincipal principal,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(paymentService.history(principal.getId(), page, size));
    }

    @PostMapping("/bookings/{bookingId}/processing")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PaymentResponse> markProcessing(@PathVariable Long bookingId,
                                                       @AuthenticationPrincipal AuthPrincipal principal,
                                                       @Valid @RequestBody(required = false) PaymentStatusUpdateRequest request) {
        return ApiResponse.success("Payment processing",
                paymentService.markProcessing(bookingId, principal.getId(), request));
    }

    @PostMapping("/bookings/{bookingId}/razorpay/order")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<RazorpayOrderResponse> createRazorpayOrder(@PathVariable Long bookingId,
                                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success("Razorpay order created",
                paymentService.createRazorpayOrder(bookingId, principal.getId()));
    }

    @PostMapping("/bookings/{bookingId}/razorpay/verify")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<?> verifyRazorpayPayment(@PathVariable Long bookingId,
                                                @AuthenticationPrincipal AuthPrincipal principal,
                                                @Valid @RequestBody RazorpayVerifyRequest request) {
        return ApiResponse.success("Payment verified",
                paymentService.verifyRazorpayPayment(bookingId, principal.getId(), request));
    }

    @PostMapping("/bookings/{bookingId}/success")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<?> markSuccess(@PathVariable Long bookingId,
                                      @AuthenticationPrincipal AuthPrincipal principal,
                                      @Valid @RequestBody(required = false) PaymentStatusUpdateRequest request) {
        return ApiResponse.success("Booking Successful",
                paymentService.markSuccessful(bookingId, principal.getId(), request));
    }

    @PostMapping("/bookings/{bookingId}/failed")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PaymentResponse> markFailed(@PathVariable Long bookingId,
                                                   @AuthenticationPrincipal AuthPrincipal principal,
                                                   @Valid @RequestBody(required = false) PaymentStatusUpdateRequest request) {
        return ApiResponse.success("Payment failed",
                paymentService.markFailed(bookingId, principal.getId(), request));
    }

    @GetMapping("/{paymentId}/refunds")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ApiResponse<List<RefundResponse>> refunds(@PathVariable Long paymentId) {
        return ApiResponse.success(refundService.byPayment(paymentId));
    }

    /** All refunds for the currently logged-in user — used by /payments/refunds/my */
    @GetMapping("/refunds/my")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<RefundResponse>> myRefunds(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.success(refundService.myRefunds(principal.getId()));
    }
}
