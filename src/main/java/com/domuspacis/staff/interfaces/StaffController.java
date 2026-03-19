package com.domuspacis.staff.interfaces;

import com.domuspacis.shared.util.ApiResponse;
import com.domuspacis.staff.application.PayrollService;
import com.domuspacis.staff.application.ScheduleService;
import com.domuspacis.staff.application.StaffService;
import com.domuspacis.staff.domain.*;
import com.domuspacis.staff.interfaces.dto.StaffDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Management", description = "Employees, roles, schedules and payroll")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class StaffController {

    private final StaffService    staffService;
    private final ScheduleService scheduleService;
    private final PayrollService  payrollService;

    // ── Employees ─────────────────────────────────────────────────────────────

    @PostMapping("/employees")
    @Operation(summary = "Create employee profile")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody CreateEmployeeRequest req) {
        Employee e = staffService.createEmployee(req.fullName(), req.nationalId(), req.phone(),
                req.roleId(), req.department(), req.contractType(),
                req.hireDate(), req.baseSalary(), req.bankAccount(), req.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created", toEmployeeResponse(e)));
    }

    @GetMapping("/employees")
    @Operation(summary = "List active employees")
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                staffService.listActive(pageable).map(this::toEmployeeResponse)));
    }

    @GetMapping("/employees/search")
    @Operation(summary = "Search employees by name")
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> search(
            @RequestParam String q, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                staffService.searchByName(q, pageable).map(this::toEmployeeResponse)));
    }

    @GetMapping("/employees/{id}")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toEmployeeResponse(staffService.getById(id))));
    }

    @GetMapping("/employees/department/{dept}")
    @Operation(summary = "List employees by department")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> byDepartment(@PathVariable String dept) {
        return ResponseEntity.ok(ApiResponse.success(
                staffService.listByDepartment(dept).stream().map(this::toEmployeeResponse).toList()));
    }

    @PatchMapping("/employees/{id}/role")
    @Operation(summary = "Assign role to employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> assignRole(
            @PathVariable UUID id, @RequestParam UUID roleId) {
        return ResponseEntity.ok(ApiResponse.success(
                toEmployeeResponse(staffService.assignRole(id, roleId))));
    }

    @PatchMapping("/employees/{id}/salary")
    @Operation(summary = "Update employee base salary")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateSalary(
            @PathVariable UUID id, @Valid @RequestBody UpdateSalaryRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                toEmployeeResponse(staffService.updateSalary(id, req.newSalary()))));
    }

    @DeleteMapping("/employees/{id}/terminate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Terminate an employee")
    public ResponseEntity<ApiResponse<Void>> terminate(@PathVariable UUID id) {
        staffService.terminateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee terminated", null));
    }

    // ── Roles ─────────────────────────────────────────────────────────────────

    @PostMapping("/roles")
    @Operation(summary = "Create employee role")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest req) {
        EmployeeRole role = staffService.createRole(req.title(), req.description(), req.permissions());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created", toRoleResponse(role)));
    }

    @GetMapping("/roles")
    @Operation(summary = "List all roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success(
                staffService.listRoles().stream().map(this::toRoleResponse).toList()));
    }

    // ── Schedules ─────────────────────────────────────────────────────────────

    @PostMapping("/schedules")
    @Operation(summary = "Create weekly work schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @Valid @RequestBody CreateScheduleRequest req) {
        List<ScheduleService.ShiftInput> shifts = req.shifts().stream()
                .map(s -> new ScheduleService.ShiftInput(s.dayOfWeek(), s.startTime(), s.endTime()))
                .toList();
        WorkSchedule ws = scheduleService.createSchedule(req.employeeId(), req.weekStartDate(), shifts);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Schedule created", toScheduleResponse(ws)));
    }

    @GetMapping("/schedules/employee/{employeeId}")
    @Operation(summary = "List schedules for an employee")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.listByEmployee(employeeId).stream().map(this::toScheduleResponse).toList()));
    }

    @GetMapping("/schedules/week")
    @Operation(summary = "List all schedules for a week")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> byWeek(
            @RequestParam String weekStart) {
        return ResponseEntity.ok(ApiResponse.success(
                scheduleService.listByWeek(java.time.LocalDate.parse(weekStart))
                        .stream().map(this::toScheduleResponse).toList()));
    }

    @DeleteMapping("/schedules/{id}")
    @Operation(summary = "Delete a schedule")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable UUID id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success("Schedule deleted", null));
    }

    // ── Payroll ───────────────────────────────────────────────────────────────

    @PostMapping("/payroll/compute")
    @Operation(summary = "Compute payroll for one employee")
    public ResponseEntity<ApiResponse<PayrollResponse>> computeOne(
            @Valid @RequestBody ComputePayrollRequest req) {
        PayrollRecord r = payrollService.computePayroll(req.employeeId(), YearMonth.parse(req.period()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payroll computed", toPayrollResponse(r)));
    }

    @PostMapping("/payroll/compute-all")
    @Operation(summary = "Compute payroll for all active employees")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> computeAll(@RequestParam String period) {
        List<PayrollRecord> records = payrollService.computePayrollForAllActive(YearMonth.parse(period));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payroll computed for " + records.size() + " employees",
                        records.stream().map(this::toPayrollResponse).toList()));
    }

    @GetMapping("/payroll/period/{period}")
    @Operation(summary = "List payroll records for a period")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> byPeriod(@PathVariable String period) {
        return ResponseEntity.ok(ApiResponse.success(
                payrollService.listByPeriod(YearMonth.parse(period))
                        .stream().map(this::toPayrollResponse).toList()));
    }

    @GetMapping("/payroll/{id}")
    @Operation(summary = "Get payroll record by ID")
    public ResponseEntity<ApiResponse<PayrollResponse>> getPayroll(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toPayrollResponse(payrollService.getById(id))));
    }

    @PatchMapping("/payroll/{id}/approve")
    @Operation(summary = "Approve payroll record")
    public ResponseEntity<ApiResponse<PayrollResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toPayrollResponse(payrollService.approvePayroll(id))));
    }

    @PostMapping("/payroll/approve-all")
    @Operation(summary = "Approve all draft payroll records for a period")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> approveAll(@RequestParam String period) {
        return ResponseEntity.ok(ApiResponse.success(
                payrollService.approveAllForPeriod(YearMonth.parse(period))
                        .stream().map(this::toPayrollResponse).toList()));
    }

    @PatchMapping("/payroll/{id}/pay")
    @Operation(summary = "Mark payroll record as paid")
    public ResponseEntity<ApiResponse<PayrollResponse>> markPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toPayrollResponse(payrollService.markPaid(id))));
    }

    // ── mappers ───────────────────────────────────────────────────────────────

    private EmployeeResponse toEmployeeResponse(Employee e) {
        return new EmployeeResponse(e.getId(), e.getFullName(), e.getNationalId(), e.getPhone(),
                e.getRole() != null ? e.getRole().getTitle() : null,
                e.getDepartment(), e.getContractType().name(),
                e.getHireDate(), e.getBaseSalary(), e.getIsActive(), e.getCreatedAt());
    }

    private RoleResponse toRoleResponse(EmployeeRole r) {
        return new RoleResponse(r.getId(), r.getTitle(), r.getDescription(), r.getPermissions());
    }

    private ScheduleResponse toScheduleResponse(WorkSchedule ws) {
        List<ShiftResponse> shifts = ws.getShifts().stream()
                .map(s -> new ShiftResponse(s.getId(), s.getDayOfWeek().name(), s.getStartTime(), s.getEndTime()))
                .toList();
        return new ScheduleResponse(ws.getId(), ws.getEmployee().getId(),
                ws.getEmployee().getFullName(), ws.getWeekStartDate(), shifts);
    }

    private PayrollResponse toPayrollResponse(PayrollRecord r) {
        String period = YearMonth.of(r.getPeriodYear(), r.getPeriodMonth()).toString();
        return new PayrollResponse(r.getId(), r.getEmployee().getId(),
                r.getEmployee().getFullName(), period,
                r.getGrossSalary(), r.getDeductions(), r.getNetSalary(),
                r.getTaxWithheld(), r.getStatus().name(), r.getPaidAt());
    }
}
