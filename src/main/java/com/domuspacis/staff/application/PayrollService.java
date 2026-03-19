package com.domuspacis.staff.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import com.domuspacis.staff.domain.*;
import com.domuspacis.staff.infrastructure.EmployeeRepository;
import com.domuspacis.staff.infrastructure.PayrollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayrollService {

    private final PayrollRepository  payrollRepository;
    private final EmployeeRepository employeeRepository;

    /** Withholding tax rate for Rwanda (PAYE) – configurable in future via TaxRuleConfig */
    private static final BigDecimal PAYE_RATE       = new BigDecimal("0.30");
    private static final BigDecimal RSSB_EMPLOYEE   = new BigDecimal("0.05");
    private static final BigDecimal RSSB_EMPLOYER   = new BigDecimal("0.05");

    public PayrollRecord computePayroll(UUID employeeId, YearMonth period) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        if (payrollRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(
                employeeId, period.getYear(), period.getMonthValue()).isPresent()) {
            throw new BusinessRuleViolationException(
                    "Payroll already computed for " + employee.getFullName() + " period " + period);
        }

        BigDecimal gross       = employee.getBaseSalary() != null
                                 ? employee.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal rssbDed     = gross.multiply(RSSB_EMPLOYEE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxableInc  = gross.subtract(rssbDed);
        BigDecimal taxWithheld = taxableInc.multiply(PAYE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDed    = rssbDed.add(taxWithheld);
        BigDecimal net         = gross.subtract(totalDed).setScale(2, RoundingMode.HALF_UP);

        PayrollRecord record = PayrollRecord.builder()
                .employee(employee)
                .periodYear(period.getYear())
                .periodMonth(period.getMonthValue())
                .grossSalary(gross)
                .deductions(totalDed)
                .netSalary(net)
                .taxWithheld(taxWithheld)
                .status(PayrollStatus.DRAFT)
                .build();

        return payrollRepository.save(record);
    }

    public List<PayrollRecord> computePayrollForAllActive(YearMonth period) {
        return employeeRepository.findByIsActiveTrue(Pageable.unpaged()).stream()
                .map(e -> {
                    try { return computePayroll(e.getId(), period); }
                    catch (BusinessRuleViolationException ex) {
                        log.warn("Payroll already exists for {}: {}", e.getFullName(), ex.getMessage());
                        return payrollRepository.findByEmployeeIdAndPeriodYearAndPeriodMonth(
                                e.getId(), period.getYear(), period.getMonthValue()).orElseThrow();
                    }
                }).toList();
    }

    @Audited("APPROVE_PAYROLL")
    public PayrollRecord approvePayroll(UUID recordId) {
        PayrollRecord record = findById(recordId);
        if (record.getStatus() != PayrollStatus.DRAFT)
            throw new BusinessRuleViolationException("Only DRAFT payroll records can be approved");
        record.setStatus(PayrollStatus.APPROVED);
        return payrollRepository.save(record);
    }

    @Audited("APPROVE_PAYROLL")
    public List<PayrollRecord> approveAllForPeriod(YearMonth period) {
        return payrollRepository
                .findByPeriodYearAndPeriodMonth(period.getYear(), period.getMonthValue())
                .stream()
                .filter(r -> r.getStatus() == PayrollStatus.DRAFT)
                .map(r -> { r.setStatus(PayrollStatus.APPROVED); return payrollRepository.save(r); })
                .toList();
    }

    @Audited("PAY_PAYROLL")
    public PayrollRecord markPaid(UUID recordId) {
        PayrollRecord record = findById(recordId);
        if (record.getStatus() != PayrollStatus.APPROVED)
            throw new BusinessRuleViolationException("Only APPROVED records can be marked PAID");
        record.setStatus(PayrollStatus.PAID);
        record.setPaidAt(Instant.now());
        return payrollRepository.save(record);
    }

    @Transactional(readOnly = true)
    public PayrollRecord getById(UUID id) { return findById(id); }

    @Transactional(readOnly = true)
    public List<PayrollRecord> listByPeriod(YearMonth period) {
        return payrollRepository.findByPeriodYearAndPeriodMonth(period.getYear(), period.getMonthValue());
    }

    @Transactional(readOnly = true)
    public Page<PayrollRecord> listByStatus(PayrollStatus status, Pageable pageable) {
        return payrollRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalNetPaidForMonth(YearMonth period) {
        return payrollRepository.totalNetPaidForMonth(period.getYear(), period.getMonthValue());
    }

    private PayrollRecord findById(UUID id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRecord", id));
    }
}
