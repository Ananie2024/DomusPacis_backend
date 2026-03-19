package com.domuspacis.staff.domain;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "employee_roles", indexes = { @Index(name = "idx_emp_role_title", columnList = "title") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeRole extends BaseEntity {
    @Column(name = "title", nullable = false, length = 100) private String title;
    @Column(name = "description", columnDefinition = "TEXT") private String description;
    @ElementCollection
    @CollectionTable(name = "employee_role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission") @Builder.Default private List<String> permissions = new ArrayList<>();
}
