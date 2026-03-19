package com.domuspacis.staff.domain;
import com.domuspacis.auth.domain.User;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name = "employees", indexes = {
    @Index(name = "idx_emp_user",       columnList = "user_id"),
    @Index(name = "idx_emp_dept",       columnList = "department"),
    @Index(name = "idx_emp_contract",   columnList = "contract_type"),
    @Index(name = "idx_emp_national",   columnList = "national_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") private User user;
    @Column(name = "full_name", nullable = false, length = 255) private String fullName;
    @Column(name = "national_id", length = 50) private String nationalId;
    @Column(name = "phone", length = 50) private String phone;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id") private EmployeeRole role;
    @Column(name = "department", length = 100) private String department;
    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 20) private ContractType contractType;
    @Column(name = "hire_date") private LocalDate hireDate;
    @Column(name = "base_salary", precision = 12, scale = 2) private BigDecimal baseSalary;
    @Column(name = "bank_account", length = 100) private String bankAccount;
    @Column(name = "is_active", nullable = false) @Builder.Default private Boolean isActive = true;
}
