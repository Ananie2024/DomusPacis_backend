package com.domuspacis.inventory.domain;
import com.domuspacis.booking.domain.Booking;
import com.domuspacis.customer.domain.Customer;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "food_orders", indexes = {
    @Index(name = "idx_fo_customer", columnList = "customer_id"),
    @Index(name = "idx_fo_booking",  columnList = "booking_id"),
    @Index(name = "idx_fo_status",   columnList = "status"),
    @Index(name = "idx_fo_ordered",  columnList = "ordered_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FoodOrder extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id") private Booking booking;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private FoodOrderStatus status = FoodOrderStatus.PENDING;
    @OneToMany(mappedBy = "foodOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<FoodOrderItem> items = new ArrayList<>();
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(name = "delivery_location", length = 255) private String deliveryLocation;
    @Column(name = "ordered_at") @Builder.Default private Instant orderedAt = Instant.now();
}
