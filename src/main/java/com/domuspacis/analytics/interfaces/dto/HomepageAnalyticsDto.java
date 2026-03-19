package com.domuspacis.analytics.interfaces.dto;

import java.util.List;

public record HomepageAnalyticsDto(

        // ── Core Highlight Stats ─────────────────────
        List<StatItem> stats,

        // ── Optional: Testimonials Preview ───────────
        List<TestimonialItem> testimonials,

        // ── Optional: Quick Insights ─────────────────
        QuickInsights insights

) {

    public record StatItem(
            String label,
            String value,
            String icon
    ) {}

    public record TestimonialItem(
            String quote,
            String name,
            String role
    ) {}

    public record QuickInsights(
            String occupancyRate,
            String monthlyRevenue,
            String customerGrowth
    ) {}
}
