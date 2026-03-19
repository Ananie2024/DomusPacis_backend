package com.domuspacis.inventory.domain;
import com.domuspacis.shared.domain.BaseEntity;
import com.domuspacis.staff.domain.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_item",   columnList = "item_id"),
    @Index(name = "idx_stock_date",   columnList = "movement_date"),
    @Index(name = "idx_stock_type",   columnList = "movement_type"),
    @Index(name = "idx_stock_by",     columnList = "recorded_by")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false) private InventoryItem item;
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20) private MovementType movementType;
    @Column(name = "quantity", nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(name = "movement_date", nullable = false) private LocalDate movementDate;
    @Column(name = "reference_note", length = 500) private String referenceNote;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by") private Employee recordedBy;
}
