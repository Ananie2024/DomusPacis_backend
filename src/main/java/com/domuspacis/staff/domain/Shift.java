package com.domuspacis.staff.domain;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.util.UUID;
@Entity
@Table(name = "shifts", indexes = { @Index(name = "idx_shift_schedule", columnList = "schedule_id") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shift {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false) private WorkSchedule schedule;
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10) private DayOfWeek dayOfWeek;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time",   nullable = false) private LocalTime endTime;
    public enum DayOfWeek { MON, TUE, WED, THU, FRI, SAT, SUN }
}
