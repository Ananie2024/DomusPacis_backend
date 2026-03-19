package com.domuspacis.finance.domain;

import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "financial_reports", indexes = {
    @Index(name = "idx_report_type",   columnList = "report_type"),
    @Index(name = "idx_report_period", columnList = "period"),
    @Index(name = "idx_report_gen_by", columnList = "generated_by")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialReport extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    @Column(name = "period", nullable = false, length = 20)
    private String period;

    @Column(name = "total_revenue", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_expenses", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    @Column(name = "net_income", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal netIncome = BigDecimal.ZERO;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "generated_by", columnDefinition = "VARCHAR(36)")
    private UUID generatedBy;
}
