package com.domuspacis.analytics.interfaces;

import com.domuspacis.analytics.application.StatisticsService;
import com.domuspacis.shared.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Statistics", description = "KPI dashboards, occupancy, revenue trends")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','FINANCE')")
public class AnalyticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "Overview KPI dashboard — today's check-ins, pending bookings, monthly revenue")
    public ResponseEntity<ApiResponse<StatisticsService.OverviewKpi>> overview() {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getOverviewKpis()));
    }

    @GetMapping("/occupancy")
    @Operation(summary = "Occupancy rate for a date range")
    public ResponseEntity<ApiResponse<StatisticsService.OccupancyStats>> occupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getOccupancyStats(from, to)));
    }

    @GetMapping("/revenue/monthly")
    @Operation(summary = "Monthly revenue vs expense trend for a year")
    public ResponseEntity<ApiResponse<List<StatisticsService.MonthlyRevenueStat>>> monthlyTrend(
            @RequestParam(required = false) Integer year) {
        int resolvedYear = (year != null) ? year : java.time.Year.now().getValue();
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getMonthlyRevenueTrend(resolvedYear)));
    }

    @GetMapping("/revenue/by-source")
    @Operation(summary = "Revenue breakdown by source type (BOOKING, FOOD_SERVICE, OTHER)")
    public ResponseEntity<ApiResponse<List<StatisticsService.RevenueBySource>>> bySource(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getRevenueBySource(from, to)));
    }

    @GetMapping("/services/popularity")
    @Operation(summary = "Service popularity ranked by booking count")
    public ResponseEntity<ApiResponse<List<StatisticsService.ServicePopularityStat>>> popularity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getServicePopularity(from, to)));
    }

    @GetMapping("/customers")
    @Operation(summary = "Customer activity summary for a date range")
    public ResponseEntity<ApiResponse<StatisticsService.CustomerActivitySummary>> customerActivity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getCustomerActivity(from, to)));
    }
}
