package com.domuspacis.finance;

import com.domuspacis.AbstractIntegrationTest;
import com.domuspacis.finance.application.ExpenseService;
import com.domuspacis.finance.application.FinancialReportService;
import com.domuspacis.finance.domain.ExpenseCategory;
import com.domuspacis.finance.domain.FinancialReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Finance Integration Tests")
class FinanceIntegrationTest extends AbstractIntegrationTest {

    @Autowired ExpenseService         expenseService;
    @Autowired FinancialReportService reportService;

    @Test
    @DisplayName("Expense is persisted with correct values")
    void logExpense_persistsCorrectly() {
        var expense = expenseService.logExpense(
                ExpenseCategory.UTILITIES,
                "Monthly electricity bill",
                new BigDecimal("150000"),
                LocalDate.now(),
                null,
                "ELEC-2025-01");

        assertThat(expense.getId()).isNotNull();
        assertThat(expense.getAmount()).isEqualByComparingTo("150000");
        assertThat(expense.getCategory()).isEqualTo(ExpenseCategory.UTILITIES);
        assertThat(expense.getReceiptReference()).isEqualTo("ELEC-2025-01");
    }

    @Test
    @DisplayName("Monthly financial report generates with zero values when no transactions")
    void generateMonthlyReport_noTransactions_returnsZeros() {
        FinancialReport report = reportService.generateMonthlyReport(
                YearMonth.of(2020, 1), null);

        assertThat(report.getId()).isNotNull();
        assertThat(report.getPeriod()).isEqualTo("2020-01");
        assertThat(report.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getNetIncome()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Quarterly report computes correct period string")
    void generateQuarterlyReport_correctPeriod() {
        FinancialReport report = reportService.generateQuarterlyReport(2025, 2, null);
        assertThat(report.getPeriod()).isEqualTo("2025-Q2");
        assertThat(report.getReportType().name()).isEqualTo("QUARTERLY");
    }

    @Test
    @DisplayName("Annual report covers full year")
    void generateAnnualReport_correctPeriod() {
        FinancialReport report = reportService.generateAnnualReport(2024, null);
        assertThat(report.getPeriod()).isEqualTo("2024");
        assertThat(report.getReportType().name()).isEqualTo("ANNUAL");
    }
}
