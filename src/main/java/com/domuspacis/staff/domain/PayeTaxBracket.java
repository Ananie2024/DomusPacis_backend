package com.domuspacis.staff.domain;

import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Configurable PAYE (Pay-As-You-Earn) tax bracket.
 *
 * Replaces the hardcoded Rwanda PAYE bands in {@link PayrollService}.
 * Brackets are versioned by effective date, so tax law changes can
 * be applied without redeploying the application.
 *
 * Rwanda PAYE structure (2024/2025):
 *   Band 1: 0 – 360,000 RWF       → 0%
 *   Band 2: 360,001 – 720,000 RWF  → 20%
 *   Band 3: > 720,000 RWF          → 30%
 */
@Entity
@Table(name = "paye_tax_brackets", indexes = {
    @Index(name = "idx_paye_bracket_active", columnList = "is_active, effective_from, effective_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayeTaxBracket extends BaseEntity {

    @Column(name = "lower_bound", nullable = false, precision = 12, scale = 2)
    private BigDecimal lowerBound;

    @Column(name = "upper_bound", precision = 12, scale = 2)
    private BigDecimal upperBound;

    @Column(name = "rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal rate;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}