package com.domuspacis.inventory.domain;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "menu_items", indexes = {
    @Index(name = "idx_menu_category",  columnList = "category"),
    @Index(name = "idx_menu_available", columnList = "is_available"),
    @Index(name = "idx_menu_name",      columnList = "name")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuItem extends BaseEntity {
    @Column(name = "name", nullable = false, length = 255) private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20) private MenuCategory category;
    @Column(name = "description", columnDefinition = "TEXT") private String description;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "is_available", nullable = false) @Builder.Default private Boolean isAvailable = true;
    @ManyToMany
    @JoinTable(name = "menu_item_ingredients",
        joinColumns        = @JoinColumn(name = "menu_item_id"),
        inverseJoinColumns = @JoinColumn(name = "inventory_item_id"))
    @Builder.Default private List<InventoryItem> ingredients = new ArrayList<>();
    public enum MenuCategory { FOOD, BEVERAGE }
}
