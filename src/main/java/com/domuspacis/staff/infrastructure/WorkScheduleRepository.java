package com.domuspacis.staff.infrastructure;
import com.domuspacis.staff.domain.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {
    Optional<WorkSchedule> findByEmployeeIdAndWeekStartDate(UUID employeeId, LocalDate weekStart);
    List<WorkSchedule> findByEmployeeId(UUID employeeId);
    List<WorkSchedule> findByWeekStartDate(LocalDate weekStart);
}
