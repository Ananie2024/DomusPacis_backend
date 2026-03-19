package com.domuspacis.analytics.interfaces.domainprojection;
import java.math.BigDecimal;

public class DomainProjectionModel {

    public record HomepageAnalyticsData(
            int yearsOfService,
            long totalCustomers,
            long totalBookings,
            long totalAssets,
            BigDecimal monthlyRevenue
    ) {}
}
