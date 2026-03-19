package com.domuspacis.finance.domain;

import com.domuspacis.booking.domain.Booking;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "invoices",
    indexes = {
        @Index(name = "idx_invoice_booking",  columnList = "booking_id", unique = true),
        @Index(name = "idx_invoice_number",   columnList = "invoice_number", unique = true),
        @Index(name = "idx_invoice_status",   columnList = "status"),
        @Index(name = "idx_invoice_issued",   columnList = "issued_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "tax_record_id", columnDefinition = "VARCHAR(36)")
    private java.util.UUID taxRecordId;

    public enum InvoiceStatus { DRAFT, ISSUED, PAID, VOID }
}
