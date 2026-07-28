package com.domuspacis.finance.application;

import com.domuspacis.finance.domain.FinancialReport;
import com.domuspacis.finance.domain.ReportType;
import com.domuspacis.finance.infrastructure.ExpenseRepository;
import com.domuspacis.finance.infrastructure.FinancialReportRepository;
import com.domuspacis.finance.infrastructure.RevenueTransactionRepository;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@DisplayName("FinancialReportService Unit Tests")
class FinancialReportServiceTest {

    @Mock private FinancialReportRepository financialReportRepository;
    @Mock private RevenueTransactionRepository revenueTransactionRepository;
    @Mock private ExpenseRepository expenseRepository;

    @InjectMocks private FinancialReportService financialReportService;

    private FinancialReport testReport;
    private UUID reportId;

    @BeforeEach
    void setUp() {
        reportId = UUID.randomUUID();

        testReport = FinancialReport.builder()
                .reportType(ReportType.MONTHLY)
                .period("2025-01")
                .totalRevenue(new BigDecimal("500000"))
                .totalExpenses(new BigDecimal("300000"))
                .netIncome(new BigDecimal("200000"))
                .build();
        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testReport, reportId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("generateMonthlyReport - generates report with correct period")
    void generateMonthlyReport_correctPeriod() {
        when(expenseRepository.sumByDateRange(any(), any())).thenReturn(new BigDecimal("300000"));
        when(revenueTransactionRepository.sumByDateRange(any(), any())).thenReturn(new BigDecimal("500000"));
        when(financialReportRepository.save(any(FinancialReport.class))).thenReturn(testReport);

        var response = financialReportService.generateMonthlyReport(YearMonth.of(2025, 1), null);

        assertThat(response).isNotNull();
        assertThat(response.getPeriod()).isEqualTo("2025-01");
        assertThat(response.getReportType()).isEqualTo(ReportType.MONTHLY);
        verify(financialReportRepository).save(any(FinancialReport.class));
    }

    @Test
    @DisplayName("generateQuarterlyReport - generates report with correct period")
    void generateQuarterlyReport_correctPeriod() {
        FinancialReport quarterlyReport = FinancialReport.builder()
                .reportType(ReportType.QUARTERLY)
                .period("2025-Q2")
                .totalRevenue(new BigDecimal("500000"))
                .totalExpenses(new BigDecimal("300000"))
                .netIncome(new BigDecimal("200000"))
                .build();
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(quarterlyReport, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(expenseRepository.sumByDateRange(any(), any())).thenReturn(new BigDecimal("300000"));
        when(revenueTransactionRepository.sumByDateRange(any(), any())).thenReturn(new BigDecimal("500000"));
        when(financialReportRepository.save(any(FinancialReport.class))).thenReturn(quarterlyReport);

        var response = financialReportService.generateQuarterlyReport(2025, 2, null);

        assertThat(response).isNotNull();
        assertThat(response.getPeriod()).isEqualTo("2025-Q2");
        assertThat(response.getReportType()).isEqualTo(ReportType.QUARTERLY);
        verify(financialReportRepository).save(any(FinancialReport.class));
    }

    @Test
    @DisplayName("generateAnnualReport - generates report with correct period")
    void generateAnnualReport_correctPeriod() {
        FinancialReport annualReport = FinancialReport.builder()
                .reportType(ReportType.ANNUAL)
                .period("2025")
                .totalRevenue(new BigDecimal("500000"))
                .totalExpenses(new BigDecimal("300000"))
                .netIncome(new BigDecimal("200000"))
                .build();
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(annualReport, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(expenseRepository.sumByDateRange(any(), any())).thenReturn(new BigDecimal("300000"));
        when(revenueTransactionRepository.sumByDateRange(any(), any())).thenReturn(new BigDecimal("500000"));
        when(financialReportRepository.save(any(FinancialReport.class))).thenReturn(annualReport);

        var response = financialReportService.generateAnnualReport(2025, null);

        assertThat(response).isNotNull();
        assertThat(response.getPeriod()).isEqualTo("2025");
        assertThat(response.getReportType()).isEqualTo(ReportType.ANNUAL);
        verify(financialReportRepository).save(any(FinancialReport.class));
    }

    @Test
    @DisplayName("getById - returns report when exists")
    void getById_existingReport_returnsReport() {
        when(financialReportRepository.findById(reportId)).thenReturn(Optional.of(testReport));

        var response = financialReportService.getById(reportId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(reportId);
    }

    @Test
    @DisplayName("getById - throws exception when report not found")
    void getById_nonExistingReport_throwsException() {
        when(financialReportRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> financialReportService.getById(reportId));
    }

    @Test
    @DisplayName("list - returns paginated reports")
    void list_returnsPaginatedReports() {
        Pageable pageable = mock(Pageable.class);
        Page<FinancialReport> reportPage = new PageImpl<>(List.of(testReport), pageable, 1);
        when(financialReportRepository.findAllByOrderByGeneratedAtDesc(pageable)).thenReturn(reportPage);

        var response = financialReportService.list(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByType - returns reports by type")
    void listByType_returnsReportsByType() {
        when(financialReportRepository.findByReportTypeOrderByPeriodDesc(ReportType.MONTHLY)).thenReturn(List.of(testReport));

        var response = financialReportService.listByType(ReportType.MONTHLY);

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getReportType()).isEqualTo(ReportType.MONTHLY);
    }
}