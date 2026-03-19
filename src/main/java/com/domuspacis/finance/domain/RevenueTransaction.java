package com.domuspacis.finance.domain;

import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "revenue_transactions", indexes = {
    @Index(name = "idx_rev_source_type", columnList = "source_type"),
    @Index(name = "idx_rev_date",        columnList = "transaction_date"),
    @Index(name = "idx_rev_source_id",   columnList = "source_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RevenueTransaction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private RevenueSourceType sourceType;

    @Column(name = "source_id", columnDefinition = "VARCHAR(36)")
    private UUID sourceId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "RWF";

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "description", length = 500)
    private String description;
}
