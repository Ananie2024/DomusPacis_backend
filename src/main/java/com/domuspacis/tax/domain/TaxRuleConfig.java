package com.domuspacis.tax.domain;

import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tax_rule_configs", indexes = {
    @Index(name = "idx_tax_rule_type",   columnList = "tax_type"),
    @Index(name = "idx_tax_rule_active", columnList = "is_active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxRuleConfig extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20) private TaxType taxType;

    @Column(name = "rate", nullable = false, precision = 6, scale = 4) private BigDecimal rate;
    @Column(name = "description", length = 500) private String description;
    @Column(name = "effective_from", nullable = false) private LocalDate effectiveFrom;
    @Column(name = "effective_to") private LocalDate effectiveTo;
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
}
