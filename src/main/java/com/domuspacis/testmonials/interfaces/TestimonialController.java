package com.domuspacis.testmonials.interfaces;

import com.domuspacis.shared.util.ApiResponse;
import com.domuspacis.testmonials.application.TestimonialService;
import com.domuspacis.testmonials.domain.Testimonial;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/testimonials")
@RequiredArgsConstructor
@Tag(name = "Testimonials", description = "Guest testimonial management")
@SecurityRequirement(name = "bearerAuth")
public class TestimonialController {

    private final TestimonialService testimonialService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "Create a new testimonial")
    public ResponseEntity<ApiResponse<TestimonialResponse>> create(
            @Valid @RequestBody CreateTestimonialRequest req) {
        Testimonial t = testimonialService.create(req.quote(), req.authorName(), req.authorRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Testimonial created", toResponse(t)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "List all testimonials (paginated)")
    public ResponseEntity<ApiResponse<Page<TestimonialResponse>>> listAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                testimonialService.listAll(pageable).map(this::toResponse)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "Get testimonial by ID")
    public ResponseEntity<ApiResponse<TestimonialResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(testimonialService.getById(id))));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Approve a testimonial for public display")
    public ResponseEntity<ApiResponse<TestimonialResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Testimonial approved", toResponse(testimonialService.approve(id))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a testimonial")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        testimonialService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Testimonial deleted", null));
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record CreateTestimonialRequest(
            @NotBlank String quote,
            @NotBlank String authorName,
            String authorRole
    ) {}

    public record TestimonialResponse(
            UUID id,
            String quote,
            String authorName,
            String authorRole,
            boolean approved,
            Instant createdAt
    ) {}

    private TestimonialResponse toResponse(Testimonial t) {
        return new TestimonialResponse(
                t.getId(),
                t.getQuote(),
                t.getAuthorName(),
                t.getAuthorRole(),
                t.isApproved(),
                t.getCreatedAt()
        );
    }
}