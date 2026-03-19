package com.domuspacis.testmonials.infrastructure;

import com.domuspacis.testmonials.domain.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestimonialRepository extends JpaRepository<Testimonial, UUID> {

    List<Testimonial> findTop3ByApprovedTrueOrderByCreatedAtDesc();
}