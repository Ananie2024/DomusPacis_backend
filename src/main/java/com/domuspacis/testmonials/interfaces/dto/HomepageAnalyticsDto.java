package com.domuspacis.testmonials.interfaces.dto;


import java.util.List;

public record HomepageAnalyticsDto(

        // ── Main Homepage Stats (hero section) ─────────────────────
        List<StatItem> stats,

        // ── Testimonials (dynamic, from DB) ────────────────────────
        List<TestimonialItem> testimonials,

        // ── Optional Insights (lightweight metrics) ────────────────
        QuickInsights insights

) {

    // ───────────────────────────────────────────────────────────
    // STAT ITEM (Used for homepage highlights)
    // ───────────────────────────────────────────────────────────
    public record StatItem(
            String label,   // e.g. "Guests Welcomed"
            String value,   // e.g. "1.2K+"
            String icon     // e.g. "Users" (mapped in frontend)
    ) {}

    // ───────────────────────────────────────────────────────────
    // TESTIMONIAL ITEM
    // ───────────────────────────────────────────────────────────
    public record TestimonialItem(
            String quote,   // testimonial content
            String name,    // author name
            String role     // author role (e.g. "Business Client")
    ) {}

    // ───────────────────────────────────────────────────────────
    // QUICK INSIGHTS (light metrics, optional display)
    // ───────────────────────────────────────────────────────────
    public record QuickInsights(
            String monthlyRevenue,
            String occupancyRate,
            String customerGrowth
    ) {}
}