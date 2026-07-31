package com.domuspacis.testmonials.application;

import com.domuspacis.shared.exception.ResourceNotFoundException;
import com.domuspacis.testmonials.domain.Testimonial;
import com.domuspacis.testmonials.infrastructure.TestimonialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TestimonialService {

    private final TestimonialRepository repository;

    public List<Testimonial> getHomepageTestimonials() {
        return repository.findTop3ByApprovedTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<Testimonial> listAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Testimonial getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial", id));
    }

    public Testimonial create(String quote, String authorName, String authorRole) {
        Testimonial testimonial = Testimonial.builder()
                .quote(quote)
                .authorName(authorName)
                .authorRole(authorRole)
                .approved(false)
                .build();
        Testimonial saved = repository.save(testimonial);
        log.info("Testimonial created: id={}, author={}", saved.getId(), authorName);
        return saved;
    }

    public Testimonial approve(UUID id) {
        Testimonial testimonial = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial", id));
        testimonial.setApproved(true);
        Testimonial saved = repository.save(testimonial);
        log.info("Testimonial approved: id={}", id);
        return saved;
    }

    public void delete(UUID id) {
        if (!repository.existsById(id))
            throw new ResourceNotFoundException("Testimonial", id);
        repository.deleteById(id);
        log.info("Testimonial deleted: id={}", id);
    }
}
