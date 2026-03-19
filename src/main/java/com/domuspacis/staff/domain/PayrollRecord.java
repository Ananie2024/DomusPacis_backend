package com.domuspacis.staff.domain;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity
@Table(name = "payroll_records", indexes = {
    @Index(name = "idx_payroll_emp",    columnList = "employee_id"),
    @Index(name = "idx_payroll_period", columnList = "period_year, period_month"),
    @Index(name = "idx_payroll_status", columnList = "status"),
    @Index(name = "idx_payroll_unique", columnList = "employee_id, period_year, period_month", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @Column(name = "period_year",  nullable = false) private int periodYear;
    @Column(name = "period_month", nullable = false) private int periodMonth;
    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2) private BigDecimal grossSalary;
    @Column(name = "deductions", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal deductions = BigDecimal.ZERO;
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2) private BigDecimal netSalary;
    @Column(name = "tax_withheld", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal taxWithheld = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private PayrollStatus status = PayrollStatus.DRAFT;
    @Column(name = "paid_at") private Instant paidAt;
}
