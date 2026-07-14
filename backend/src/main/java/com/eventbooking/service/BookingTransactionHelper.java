package com.eventbooking.service;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Payment;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingTransactionHelper {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Processes a single expired booking in its own transaction (REQUIRES_NEW).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseOneExpiredBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getBookingStatus() != Booking.BookingStatus.PENDING) return;

        booking.setBookingStatus(Booking.BookingStatus.EXPIRED);
        booking.setTicketStatus(Booking.TicketStatus.EXPIRED);
        booking.setExpiredAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Use direct UPDATE to avoid SELECT FOR UPDATE lock conflict
        eventRepository.incrementAvailableSeats(booking.getEvent().getId(), booking.getQuantity());

        paymentRepository.findByBookingId(bookingId).ifPresent(payment -> {
            payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Payment timeout");
            paymentRepository.save(payment);
        });
    }
}
