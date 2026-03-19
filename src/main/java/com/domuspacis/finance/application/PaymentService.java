package com.domuspacis.finance.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.booking.domain.Booking;
import com.domuspacis.booking.infrastructure.BookingRepository;
import com.domuspacis.finance.domain.*;
import com.domuspacis.finance.infrastructure.InvoiceRepository;
import com.domuspacis.finance.infrastructure.PaymentRepository;
import com.domuspacis.finance.infrastructure.RevenueTransactionRepository;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository            paymentRepository;
    private final BookingRepository            bookingRepository;
    private final InvoiceRepository            invoiceRepository;
    private final RevenueTransactionRepository revenueTransactionRepository;

    @Audited("RECORD_PAYMENT")
    public Payment recordPayment(UUID bookingId, Payment.PaymentMethod method,
                                 java.math.BigDecimal amount, String reference) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> Payment.builder()
                        .booking(booking)
                        .currency("RWF")
                        .build());

        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setTransactionReference(reference);

        java.math.BigDecimal bookingTotal = booking.getTotalAmount() != null
                ? booking.getTotalAmount() : java.math.BigDecimal.ZERO;

        if (amount.compareTo(bookingTotal) >= 0) {
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaidAt(Instant.now());
            markInvoicePaid(bookingId);
            createRevenueTransaction(booking, amount);
        } else {
            payment.setStatus(Payment.PaymentStatus.PARTIAL);
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment recorded for booking {}: {} {}", bookingId, amount, payment.getStatus());
        return saved;
    }

    @Audited("REFUND_PAYMENT")
    public Payment refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (payment.getStatus() != Payment.PaymentStatus.PAID)
            throw new BusinessRuleViolationException("Only PAID payments can be refunded");
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getByBookingId(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for booking", bookingId));
    }

    @Transactional(readOnly = true)
    public Page<Payment> listByStatus(Payment.PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByStatus(status, pageable);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private void markInvoicePaid(UUID bookingId) {
        invoiceRepository.findByBookingId(bookingId).ifPresent(inv -> {
            inv.setStatus(Invoice.InvoiceStatus.PAID);
            invoiceRepository.save(inv);
        });
    }

    private void createRevenueTransaction(Booking booking, java.math.BigDecimal amount) {
        RevenueTransaction rt = RevenueTransaction.builder()
                .sourceType(RevenueSourceType.BOOKING)
                .sourceId(booking.getId())
                .amount(amount)
                .currency("RWF")
                .transactionDate(LocalDate.now())
                .description("Payment for booking: " + booking.getServiceAsset().getName())
                .build();
        revenueTransactionRepository.save(rt);
    }
}
