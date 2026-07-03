package com.eventbooking.controller;

import com.eventbooking.dto.request.BookingRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.dto.response.BookingResponse;
import com.eventbooking.dto.response.PagedResponse;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for ticket booking operations.
 *
 * POST   /api/bookings                   – book tickets
 * GET    /api/bookings                   – user's booking history
 * GET    /api/bookings/{id}              – booking detail
 * GET    /api/bookings/ticket/{ticketId} – lookup by ticket ID
 * PATCH  /api/bookings/{id}/cancel       – cancel booking
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BookingResponse>> bookTickets(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        BookingResponse booking = bookingService.bookTickets(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking confirmed!", booking));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> myBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getUserBookings(principal.getId(), page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingById(id, principal.getId())));
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BookingResponse>> getByTicketId(
            @PathVariable String ticketId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getByTicketId(ticketId, principal.getId())));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "User requested cancellation") String reason,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled",
                bookingService.cancelBooking(id, principal.getId(), reason)));
    }
}
