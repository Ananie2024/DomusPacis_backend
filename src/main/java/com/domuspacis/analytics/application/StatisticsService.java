package com.domuspacis.analytics.application;

import com.domuspacis.analytics.interfaces.domainprojection.DomainProjectionModel;
import com.domuspacis.booking.domain.BookingStatus;
import com.domuspacis.booking.infrastructure.BookingRepository;
import com.domuspacis.booking.infrastructure.ServiceAssetRepository;
import com.domuspacis.customer.infrastructure.CustomerRepository;
import com.domuspacis.finance.infrastructure.ExpenseRepository;
import com.domuspacis.finance.infrastructure.RevenueTransactionRepository;
import com.domuspacis.inventory.infrastructure.InventoryItemRepository;
import com.domuspacis.staff.infrastructure.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticsService {

    private final BookingRepository            bookingRepository;
    private final ServiceAssetRepository       assetRepository;
    private final CustomerRepository           customerRepository;
    private final RevenueTransactionRepository revenueRepository;
    private final ExpenseRepository            expenseRepository;
    private final InventoryItemRepository      inventoryItemRepository;
    private final EmployeeRepository           employeeRepository;

    @Value("${domuspacis.founding-year:2026}")
    private int foundingYear;

    // ── Overview KPIs ─────────────────────────────────────────────────────────

    public OverviewKpi getOverviewKpis() {
        LocalDate today = LocalDate.now();

        long todayCheckIns    = bookingRepository.findTodaysCheckIns(today).size();
        long pendingBookings  = bookingRepository.countByStatus(BookingStatus.PENDING);
        long confirmedBookings= bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long totalCustomers   = customerRepository.count();
        long activeStaff      = employeeRepository.countByIsActiveTrue();
        long lowStockAlerts   = inventoryItemRepository.findLowStock().size();

        YearMonth currentMonth = YearMonth.now();
        BigDecimal monthRevenue = revenueRepository.sumByDateRange(
                currentMonth.atDay(1), currentMonth.atEndOfMonth());
        BigDecimal monthExpenses = expenseRepository.sumByDateRange(
                currentMonth.atDay(1), currentMonth.atEndOfMonth());

        return new OverviewKpi(todayCheckIns, pendingBookings, confirmedBookings,
                totalCustomers, activeStaff, lowStockAlerts,
                monthRevenue, monthExpenses, monthRevenue.subtract(monthExpenses));
    }

    // ── Occupancy ─────────────────────────────────────────────────────────────

    public OccupancyStats getOccupancyStats(LocalDate from, LocalDate to) {
        long totalAssets = assetRepository.count();
        if (totalAssets == 0) return new OccupancyStats(0, 0, BigDecimal.ZERO, List.of());

        long totalAssetDays = totalAssets * from.until(to).getDays();
        long bookedDays     = bookingRepository.countBookingsBetween(from, to);

        BigDecimal occupancyRate = totalAssetDays > 0
                ? BigDecimal.valueOf(bookedDays)
                        .divide(BigDecimal.valueOf(totalAssetDays), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return new OccupancyStats(totalAssets, bookedDays, occupancyRate, List.of());
    }

    // ── Revenue Trends ────────────────────────────────────────────────────────

    public List<MonthlyRevenueStat> getMonthlyRevenueTrend(int year) {
        List<MonthlyRevenueStat> stats = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym   = YearMonth.of(year, m);
            BigDecimal rev = revenueRepository.sumByDateRange(ym.atDay(1), ym.atEndOfMonth());
            BigDecimal exp = expenseRepository.sumByDateRange(ym.atDay(1), ym.atEndOfMonth());
            stats.add(new MonthlyRevenueStat(ym.toString(), rev, exp, rev.subtract(exp)));
        }
        return stats;
    }

    // ── Service Popularity ────────────────────────────────────────────────────

    public List<ServicePopularityStat> getServicePopularity(LocalDate from, LocalDate to) {
        // Aggregate bookings per asset within the period
        return bookingRepository.findAll(Pageable.unpaged()).stream()
                .filter(b -> !b.getCheckInDate().isBefore(from) && !b.getCheckInDate().isAfter(to))
                .collect(java.util.stream.Collectors.groupingBy(
                        b -> b.getServiceAsset().getName(),
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ServicePopularityStat(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.bookingCount(), a.bookingCount()))
                .toList();
    }

    // ── Revenue by Source ─────────────────────────────────────────────────────

    public List<RevenueBySource> getRevenueBySource(LocalDate from, LocalDate to) {
        return revenueRepository.revenueBySourceType(from, to).stream()
                .map(row -> new RevenueBySource(row[0].toString(), (BigDecimal) row[1]))
                .toList();
    }

    // ── Customer Activity ─────────────────────────────────────────────────────

    public CustomerActivitySummary getCustomerActivity(LocalDate from, LocalDate to) {
        long totalBookings = bookingRepository.countBookingsBetween(from, to);
        long newCustomers  = customerRepository.findAll(Pageable.unpaged()).stream()
                .filter(c -> c.getCreatedAt() != null
                        && !c.getCreatedAt().isBefore(from.atStartOfDay().toInstant(java.time.ZoneOffset.UTC))
                        && !c.getCreatedAt().isAfter(to.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)))
                .count();
        long totalCustomers = customerRepository.count();
        return new CustomerActivitySummary(totalCustomers, newCustomers, totalBookings);
    }
    @Cacheable("homepageAnalytics")
    public DomainProjectionModel.HomepageAnalyticsData getHomepageAnalyticsData() {

        OverviewKpi kpi = getOverviewKpis();


        int currentYear = java.time.Year.now().getValue();

        int yearsOfService = currentYear - foundingYear;

        long totalCustomers = kpi.totalCustomers();
        long totalBookings  = bookingRepository.count();
        long totalAssets    = assetRepository.count();

        return new DomainProjectionModel.HomepageAnalyticsData(
                yearsOfService,
                totalCustomers,
                totalBookings,
                totalAssets,
                kpi.monthlyRevenue()
        );
    }

    // ── Records ───────────────────────────────────────────────────────────────

    public record OverviewKpi(long todayCheckIns, long pendingBookings, long confirmedBookings,
                               long totalCustomers, long activeStaff, long lowStockAlerts,
                               BigDecimal monthlyRevenue, BigDecimal monthlyExpenses, BigDecimal monthlyNetIncome) {}

    public record OccupancyStats(long totalAssets, long bookedDays,
                                  BigDecimal occupancyRatePercent, List<?> breakdown) {}

    public record MonthlyRevenueStat(String period, BigDecimal revenue,
                                      BigDecimal expenses, BigDecimal netIncome) {}

    public record ServicePopularityStat(String serviceName, long bookingCount) {}

    public record RevenueBySource(String sourceType, BigDecimal amount) {}

    public record CustomerActivitySummary(long totalCustomers, long newCustomers, long totalBookings) {}
}
