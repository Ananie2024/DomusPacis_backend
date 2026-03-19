package com.domuspacis.inventory.domain;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity
@Table(name = "inventory_items", indexes = {
    @Index(name = "idx_inv_category",  columnList = "category"),
    @Index(name = "idx_inv_stock",     columnList = "current_stock"),
    @Index(name = "idx_inv_supplier",  columnList = "supplier_id"),
    @Index(name = "idx_inv_name",      columnList = "name")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem extends BaseEntity {
    @Column(name = "name", nullable = false, length = 255) private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30) private ItemCategory category;
    @Column(name = "unit", nullable = false, length = 30) private String unit;
    @Column(name = "current_stock", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal currentStock = BigDecimal.ZERO;
    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 3) @Builder.Default private BigDecimal reorderLevel = BigDecimal.ZERO;
    @Column(name = "unit_cost", precision = 12, scale = 2) private BigDecimal unitCost;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id") private Supplier supplier;
}
