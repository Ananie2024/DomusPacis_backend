package com.domuspacis.staff.application;

import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import com.domuspacis.staff.domain.*;
import com.domuspacis.staff.infrastructure.EmployeeRepository;
import com.domuspacis.staff.infrastructure.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final WorkScheduleRepository scheduleRepository;
    private final EmployeeRepository     employeeRepository;

    public WorkSchedule createSchedule(UUID employeeId, LocalDate weekStart,
                                        List<ShiftInput> shiftInputs) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        if (scheduleRepository.findByEmployeeIdAndWeekStartDate(employeeId, weekStart).isPresent())
            throw new BusinessRuleViolationException(
                    "Schedule already exists for employee " + employeeId + " week " + weekStart);

        WorkSchedule schedule = WorkSchedule.builder()
                .employee(employee).weekStartDate(weekStart).build();
        schedule = scheduleRepository.save(schedule);

        for (ShiftInput si : shiftInputs) {
            Shift shift = Shift.builder()
                    .schedule(schedule)
                    .dayOfWeek(si.day())
                    .startTime(si.start())
                    .endTime(si.end())
                    .build();
            schedule.getShifts().add(shift);
        }
        return scheduleRepository.save(schedule);
    }

    public void deleteSchedule(UUID scheduleId) {
        if (!scheduleRepository.existsById(scheduleId))
            throw new ResourceNotFoundException("WorkSchedule", scheduleId);
        scheduleRepository.deleteById(scheduleId);
    }

    @Transactional(readOnly = true)
    public List<WorkSchedule> listByEmployee(UUID employeeId) {
        return scheduleRepository.findByEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public List<WorkSchedule> listByWeek(LocalDate weekStart) {
        return scheduleRepository.findByWeekStartDate(weekStart);
    }

    public record ShiftInput(Shift.DayOfWeek day, LocalTime start, LocalTime end) {}
}
