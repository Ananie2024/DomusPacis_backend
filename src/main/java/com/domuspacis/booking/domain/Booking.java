package com.domuspacis.booking.domain;

import com.domuspacis.customer.domain.Customer;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_customer",   columnList = "customer_id"),
        @Index(name = "idx_booking_asset",      columnList = "service_asset_id"),
        @Index(name = "idx_booking_status",     columnList = "status"),
        @Index(name = "idx_booking_checkin",    columnList = "check_in_date"),
        @Index(name = "idx_booking_checkout",   columnList = "check_out_date"),
        @Index(name = "idx_booking_dates",      columnList = "check_in_date, check_out_date, status")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_asset_id", nullable = false)
    private ServiceAsset serviceAsset;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "number_of_guests")
    private Integer numberOfGuests;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_by", columnDefinition = "VARCHAR(36)")
    private java.util.UUID createdBy;

    @Column(name = "updated_by", columnDefinition = "VARCHAR(36)")
    private java.util.UUID updatedBy;
}
