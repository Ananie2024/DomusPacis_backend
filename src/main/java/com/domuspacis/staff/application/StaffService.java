package com.domuspacis.staff.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.auth.domain.User;
import com.domuspacis.auth.infrastructure.UserRepository;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import com.domuspacis.staff.domain.*;
import com.domuspacis.staff.infrastructure.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StaffService {

    private final EmployeeRepository     employeeRepository;
    private final EmployeeRoleRepository roleRepository;
    private final UserRepository         userRepository;

    // ── Employee CRUD ─────────────────────────────────────────────────────────

    public Employee createEmployee(String fullName, String nationalId, String phone,
                                    UUID roleId, String department, ContractType contractType,
                                    LocalDate hireDate, BigDecimal baseSalary, String bankAccount,
                                    UUID userId) {
        if (nationalId != null && employeeRepository.findByNationalId(nationalId).isPresent())
            throw new BusinessRuleViolationException("National ID already registered: " + nationalId);

        EmployeeRole role = roleId != null
                ? roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("EmployeeRole", roleId))
                : null;

        User user = userId != null
                ? userRepository.findById(userId).orElse(null)
                : null;

        Employee emp = Employee.builder()
                .fullName(fullName).nationalId(nationalId).phone(phone)
                .role(role).department(department).contractType(contractType)
                .hireDate(hireDate).baseSalary(baseSalary).bankAccount(bankAccount)
                .user(user).isActive(true)
                .build();
        return employeeRepository.save(emp);
    }

    @Audited("ASSIGN_ROLE")
    public Employee assignRole(UUID employeeId, UUID roleId) {
        Employee emp  = findEmployeeById(employeeId);
        EmployeeRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeRole", roleId));
        emp.setRole(role);
        return employeeRepository.save(emp);
    }

    @Audited("UPDATE_SALARY")
    public Employee updateSalary(UUID employeeId, BigDecimal newSalary) {
        Employee emp = findEmployeeById(employeeId);
        emp.setBaseSalary(newSalary);
        return employeeRepository.save(emp);
    }

    @Audited("TERMINATE_EMPLOYEE")
    public void terminateEmployee(UUID employeeId) {
        Employee emp = findEmployeeById(employeeId);
        emp.setIsActive(false);
        employeeRepository.save(emp);
        log.info("Employee terminated: {}", emp.getFullName());
    }

    @Transactional(readOnly = true)
    public Employee getById(UUID id) { return findEmployeeById(id); }

    @Transactional(readOnly = true)
    public Page<Employee> listActive(Pageable pageable) {
        return employeeRepository.findByIsActiveTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Employee> searchByName(String q, Pageable pageable) {
        return employeeRepository.searchByName(q, pageable);
    }

    @Transactional(readOnly = true)
    public List<Employee> listByDepartment(String dept) {
        return employeeRepository.findByDepartment(dept);
    }

    // ── EmployeeRole CRUD ─────────────────────────────────────────────────────

    public EmployeeRole createRole(String title, String description, List<String> permissions) {
        return roleRepository.save(EmployeeRole.builder()
                .title(title).description(description).permissions(permissions).build());
    }

    @Transactional(readOnly = true)
    public List<EmployeeRole> listRoles() { return roleRepository.findAll(); }

    private Employee findEmployeeById(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }
}
