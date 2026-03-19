package com.domuspacis.home;

import com.domuspacis.analytics.application.StatisticsService;
import com.domuspacis.testmonials.application.TestimonialService;
import com.domuspacis.testmonials.interfaces.HomepageAnalyticsMapper;
import com.domuspacis.testmonials.interfaces.dto.HomepageAnalyticsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/public/home")
public class HomePageController {

    private final StatisticsService statisticsService;
    private final TestimonialService testimonialService;
    private final HomepageAnalyticsMapper homepageAnalyticsMapper;

    @GetMapping("/homepage-analytics")
    public HomepageAnalyticsDto homepageAnalytics() {

        var data = statisticsService.getHomepageAnalyticsData();
        var testimonials = testimonialService.getHomepageTestimonials();

        return homepageAnalyticsMapper.toDto(data, testimonials);
    }
}
