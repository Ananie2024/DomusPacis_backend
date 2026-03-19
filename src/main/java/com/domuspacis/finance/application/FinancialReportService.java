package com.domuspacis.finance.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.aop.annotation.TrackPerformance;
import com.domuspacis.finance.domain.FinancialReport;
import com.domuspacis.finance.domain.ReportType;
import com.domuspacis.finance.infrastructure.ExpenseRepository;
import com.domuspacis.finance.infrastructure.FinancialReportRepository;
import com.domuspacis.finance.infrastructure.RevenueTransactionRepository;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FinancialReportService {

    private final FinancialReportRepository     reportRepository;
    private final RevenueTransactionRepository  revenueRepo;
    private final ExpenseRepository             expenseRepo;

    @Audited("GENERATE_FINANCIAL_REPORT")
    @TrackPerformance(warnMs = 3000, criticalMs = 10000)
    public FinancialReport generateMonthlyReport(YearMonth month, UUID generatedBy) {
        LocalDate from = month.atDay(1);
        LocalDate to   = month.atEndOfMonth();
        String period  = month.toString();
        return buildAndSave(ReportType.MONTHLY, period, from, to, generatedBy);
    }

    @Audited("GENERATE_FINANCIAL_REPORT")
    @TrackPerformance(warnMs = 5000, criticalMs = 15000)
    public FinancialReport generateQuarterlyReport(int year, int quarter, UUID generatedBy) {
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDate from = LocalDate.of(year, startMonth, 1);
        LocalDate to   = from.plusMonths(3).minusDays(1);
        String period  = year + "-Q" + quarter;
        return buildAndSave(ReportType.QUARTERLY, period, from, to, generatedBy);
    }

    @Audited("GENERATE_FINANCIAL_REPORT")
    @TrackPerformance(warnMs = 8000, criticalMs = 20000)
    public FinancialReport generateAnnualReport(int year, UUID generatedBy) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to   = LocalDate.of(year, 12, 31);
        return buildAndSave(ReportType.ANNUAL, String.valueOf(year), from, to, generatedBy);
    }

    @Transactional(readOnly = true)
    public FinancialReport getById(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialReport", id));
    }

    @Transactional(readOnly = true)
    public Page<FinancialReport> list(Pageable pageable) {
        return reportRepository.findAllByOrderByGeneratedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<FinancialReport> listByType(ReportType type) {
        return reportRepository.findByReportTypeOrderByPeriodDesc(type);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private FinancialReport buildAndSave(ReportType type, String period,
                                          LocalDate from, LocalDate to, UUID generatedBy) {
        BigDecimal revenue  = revenueRepo.sumByDateRange(from, to);
        BigDecimal expenses = expenseRepo.sumByDateRange(from, to);
        BigDecimal net      = revenue.subtract(expenses);

        FinancialReport report = FinancialReport.builder()
                .reportType(type)
                .period(period)
                .totalRevenue(revenue)
                .totalExpenses(expenses)
                .netIncome(net)
                .generatedAt(Instant.now())
                .generatedBy(generatedBy)
                .build();

        FinancialReport saved = reportRepository.save(report);
        log.info("Financial report generated: {} {} → revenue={} expenses={} net={}",
                type, period, revenue, expenses, net);
        return saved;
    }
}
