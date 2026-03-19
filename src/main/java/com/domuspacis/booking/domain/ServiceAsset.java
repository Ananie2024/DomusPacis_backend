package com.domuspacis.booking.domain;

import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "service_assets",
    indexes = {
        @Index(name = "idx_asset_type",      columnList = "asset_type"),
        @Index(name = "idx_asset_available", columnList = "is_available"),
        @Index(name = "idx_asset_name",      columnList = "name")
    }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "asset_type", discriminatorType = DiscriminatorType.STRING, length = 30)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public abstract class ServiceAsset extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "price_per_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_unit", nullable = false, length = 20)
    private PricingUnit pricingUnit;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @ElementCollection
    @CollectionTable(name = "service_asset_images",
                     joinColumns = @JoinColumn(name = "asset_id"))
    @Column(name = "image_path")
    @Builder.Default
    private List<String> images = new ArrayList<>();
}
