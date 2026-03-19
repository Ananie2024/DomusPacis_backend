package com.domuspacis.testmonials.application;

import com.domuspacis.testmonials.domain.Testimonial;
import com.domuspacis.testmonials.infrastructure.TestimonialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestimonialService {

    private final TestimonialRepository repository;

    public List<Testimonial> getHomepageTestimonials() {
        return repository.findTop3ByApprovedTrueOrderByCreatedAtDesc();
    }
}
