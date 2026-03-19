package com.domuspacis.staff.domain;
import com.domuspacis.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "work_schedules", indexes = {
    @Index(name = "idx_sched_emp",  columnList = "employee_id"),
    @Index(name = "idx_sched_week", columnList = "employee_id, week_start_date", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkSchedule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @Column(name = "week_start_date", nullable = false) private LocalDate weekStartDate;
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<Shift> shifts = new ArrayList<>();
}
