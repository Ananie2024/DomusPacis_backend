package com.domuspacis.finance.domain;

import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expenses", indexes = {
    @Index(name = "idx_expense_category", columnList = "category"),
    @Index(name = "idx_expense_date",     columnList = "expense_date"),
    @Index(name = "idx_expense_approved", columnList = "approved_by")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expense extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "approved_by", columnDefinition = "VARCHAR(36)")
    private UUID approvedBy;

    @Column(name = "receipt_reference", length = 255)
    private String receiptReference;
}
