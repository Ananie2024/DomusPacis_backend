package com.domuspacis.tax.domain;

import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

@Entity
@Table(name = "tax_records", indexes = {
    @Index(name = "idx_tax_period",   columnList = "period_year, period_month"),
    @Index(name = "idx_tax_type",     columnList = "tax_type"),
    @Index(name = "idx_tax_status",   columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxRecord extends BaseEntity {

    @Column(name = "period_year",  nullable = false) private int periodYear;
    @Column(name = "period_month", nullable = false) private int periodMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20) private TaxType taxType;

    @Column(name = "taxable_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "tax_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "tax_due", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default private TaxStatus status = TaxStatus.DRAFT;

    @Column(name = "filed_at") private Instant filedAt;
    @Column(name = "reference_note", length = 500) private String referenceNote;

    public YearMonth getPeriod() { return YearMonth.of(periodYear, periodMonth); }
    public void setPeriod(YearMonth ym) { this.periodYear = ym.getYear(); this.periodMonth = ym.getMonthValue(); }
}
