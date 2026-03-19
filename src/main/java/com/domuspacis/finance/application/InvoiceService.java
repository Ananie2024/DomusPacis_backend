package com.domuspacis.finance.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.booking.domain.Booking;
import com.domuspacis.booking.infrastructure.BookingRepository;
import com.domuspacis.finance.domain.Invoice;
import com.domuspacis.finance.infrastructure.InvoiceRepository;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceService {

    private final InvoiceRepository  invoiceRepository;
    private final BookingRepository  bookingRepository;

    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    public Invoice generateInvoice(UUID bookingId, BigDecimal taxRate) {
        if (invoiceRepository.findByBookingId(bookingId).isPresent())
            throw new BusinessRuleViolationException("Invoice already exists for booking: " + bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        BigDecimal subtotal  = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal taxAmount = subtotal.multiply(taxRate != null ? taxRate : BigDecimal.valueOf(0.18));
        BigDecimal total     = subtotal.add(taxAmount);
        String number        = generateInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .booking(booking)
                .invoiceNumber(number)
                .issuedAt(Instant.now())
                .dueDate(LocalDate.now().plusDays(30))
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .totalAmount(total)
                .status(Invoice.InvoiceStatus.ISSUED)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} generated for booking {}", number, bookingId);
        return saved;
    }

    @Audited("VOID_INVOICE")
    public Invoice voidInvoice(UUID invoiceId) {
        Invoice invoice = findById(invoiceId);
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID)
            throw new BusinessRuleViolationException("Paid invoices cannot be voided");
        invoice.setStatus(Invoice.InvoiceStatus.VOID);
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice getById(UUID id) { return findById(id); }

    @Transactional(readOnly = true)
    public Invoice getByBookingId(UUID bookingId) {
        return invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice for booking", bookingId));
    }

    @Transactional(readOnly = true)
    public Page<Invoice> list(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Invoice> listByStatus(Invoice.InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findByStatus(status, pageable);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private Invoice findById(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    private String generateInvoiceNumber() {
        String year = DateTimeFormatter.ofPattern("yyyy").format(LocalDate.now());
        String seq  = String.format("%05d", SEQ.getAndIncrement());
        String candidate = "INV-" + year + "-" + seq;
        while (invoiceRepository.existsByInvoiceNumber(candidate)) {
            candidate = "INV-" + year + "-" + String.format("%05d", SEQ.getAndIncrement());
        }
        return candidate;
    }
}
