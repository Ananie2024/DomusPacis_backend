package com.domuspacis.inventory.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity
@Table(name = "food_order_items", indexes = {
    @Index(name = "idx_foi_order",     columnList = "food_order_id"),
    @Index(name = "idx_foi_menuitem",  columnList = "menu_item_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FoodOrderItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)") private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_order_id", nullable = false) private FoodOrder foodOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id",  nullable = false) private MenuItem menuItem;
    @Column(name = "quantity", nullable = false) private Integer quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "subtotal",   nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
}
