package com.domuspacis.finance.domain;

import com.domuspacis.booking.domain.Booking;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payment_booking",    columnList = "booking_id", unique = true),
        @Index(name = "idx_payment_status",     columnList = "status"),
        @Index(name = "idx_payment_paid_at",    columnList = "paid_at"),
        @Index(name = "idx_payment_method",     columnList = "method")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "RWF";

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_reference", length = 255)
    private String transactionReference;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum PaymentMethod { CASH, MOBILE_MONEY, BANK_TRANSFER, CARD }
    public enum PaymentStatus { PENDING, PARTIAL, PAID, REFUNDED }
}
