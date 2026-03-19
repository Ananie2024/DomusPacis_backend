package com.domuspacis.staff.interfaces.dto;

import com.domuspacis.staff.domain.ContractType;
import com.domuspacis.staff.domain.Shift;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class StaffDtos {
    private StaffDtos() {}

    // ── Employee ──────────────────────────────────────────────────────────────
    public record CreateEmployeeRequest(
        @NotBlank String fullName,
        String nationalId,
        String phone,
        UUID roleId,
        String department,
        @NotNull ContractType contractType,
        LocalDate hireDate,
        BigDecimal baseSalary,
        String bankAccount,
        UUID userId
    ) {}

    public record UpdateSalaryRequest(@NotNull @Positive BigDecimal newSalary) {}

    public record EmployeeResponse(
        UUID id,
        String fullName,
        String nationalId,
        String phone,
        String roleName,
        String department,
        String contractType,
        LocalDate hireDate,
        BigDecimal baseSalary,
        Boolean isActive,
        Instant createdAt
    ) {}

    public record EmployeeSummary(UUID id, String fullName, String department, String roleName) {}

    // ── EmployeeRole ──────────────────────────────────────────────────────────
    public record CreateRoleRequest(
        @NotBlank String title,
        String description,
        List<String> permissions
    ) {}

    public record RoleResponse(UUID id, String title, String description, List<String> permissions) {}

    // ── WorkSchedule ──────────────────────────────────────────────────────────
    public record CreateScheduleRequest(
        @NotNull UUID employeeId,
        @NotNull LocalDate weekStartDate,
        @NotEmpty List<ShiftRequest> shifts
    ) {}

    public record ShiftRequest(
        @NotNull Shift.DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
    ) {}

    public record ScheduleResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        LocalDate weekStartDate,
        List<ShiftResponse> shifts
    ) {}

    public record ShiftResponse(
        UUID id,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    ) {}

    // ── Payroll ───────────────────────────────────────────────────────────────
    public record ComputePayrollRequest(
        @NotNull UUID employeeId,
        @NotBlank String period
    ) {}

    public record PayrollResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        String period,
        BigDecimal grossSalary,
        BigDecimal deductions,
        BigDecimal netSalary,
        BigDecimal taxWithheld,
        String status,
        Instant paidAt
    ) {}
}
