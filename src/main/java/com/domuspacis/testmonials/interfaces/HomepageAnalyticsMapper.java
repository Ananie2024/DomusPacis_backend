package com.domuspacis.testmonials.interfaces;


import com.domuspacis.analytics.interfaces.domainprojection.DomainProjectionModel;
import com.domuspacis.testmonials.domain.Testimonial;
import com.domuspacis.testmonials.interfaces.dto.HomepageAnalyticsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HomepageAnalyticsMapper {

    @Mapping(target = "stats", expression = "java(mapStats(data))")
    @Mapping(target = "testimonials", expression = "java(mapTestimonials(testimonials))")
    @Mapping(target = "insights", expression = "java(mapInsights(data))")
    HomepageAnalyticsDto toDto(
            DomainProjectionModel.HomepageAnalyticsData data,
            List<Testimonial> testimonials
    );

    // ── Stats Mapping ───────────────────────────

    default List<HomepageAnalyticsDto.StatItem> mapStats(DomainProjectionModel.HomepageAnalyticsData data) {
        return List.of(
                new HomepageAnalyticsDto.StatItem("Years of Service", data.yearsOfService() + "+", "Award"),
                new HomepageAnalyticsDto.StatItem("Guests Welcomed", formatNumber(data.totalCustomers()), "Users"),
                new HomepageAnalyticsDto.StatItem("Events Hosted", formatNumber(data.totalBookings()), "Star"),
                new HomepageAnalyticsDto.StatItem("Rooms Available", String.valueOf(data.totalAssets()), "MapPin")
        );
    }

    // ── Testimonials Mapping ────────────────────

    default List<HomepageAnalyticsDto.TestimonialItem> mapTestimonials(List<Testimonial> testimonials) {
        return testimonials.stream()
                .map(t -> new HomepageAnalyticsDto.TestimonialItem(
                        t.getQuote(),
                        t.getAuthorName(),
                        t.getAuthorRole()
                ))
                .toList();
    }

    // ── Insights Mapping ────────────────────────

    default HomepageAnalyticsDto.QuickInsights mapInsights(DomainProjectionModel.HomepageAnalyticsData data) {
        return new HomepageAnalyticsDto.QuickInsights(
                formatCurrency(data.monthlyRevenue()),
                formatCurrency(data.monthlyRevenue()),
                formatNumber(data.totalCustomers())
        );
    }

    // ── Helpers ─────────────────────────────────

    default String formatNumber(long value) {
        if (value >= 1_000_000) return (value / 1_000_000) + "M+";
        if (value >= 1_000) return (value / 1_000) + "K+";
        return String.valueOf(value);
    }

    default String formatCurrency(java.math.BigDecimal value) {
        if (value == null) return "0";
        return value.setScale(0, java.math.RoundingMode.HALF_UP).toString();
    }
}