package com.domuspacis.booking.interfaces;

import com.domuspacis.booking.application.BookingService;
import com.domuspacis.booking.domain.BookingStatus;
import com.domuspacis.booking.interfaces.dto.BookingDtos.*;
import com.domuspacis.shared.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a booking request")
    public ResponseEntity<ApiResponse<BookingResponse>> create(
            @Valid @RequestBody CreateBookingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created", bookingService.createBooking(req)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "List all bookings")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.listAll(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getById(id)));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "List bookings by customer")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> byCustomer(
            @PathVariable UUID customerId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.listByCustomer(customerId, pageable)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "List bookings by status")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> byStatus(
            @PathVariable BookingStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.listByStatus(status, pageable)));
    }

    @GetMapping("/availability")
    @Operation(summary = "Check asset availability for dates")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkAvailability(
            @RequestParam UUID assetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        boolean available = bookingService.checkAvailability(assetId, checkIn, checkOut);
        return ResponseEntity.ok(ApiResponse.success(
                new AvailabilityResponse(assetId, checkIn, checkOut, available)));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "Confirm a pending booking")
    public ResponseEntity<ApiResponse<BookingResponse>> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed", bookingService.confirmBooking(id)));
    }

    @PatchMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "Record guest check-in")
    public ResponseEntity<ApiResponse<BookingResponse>> checkIn(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Checked in", bookingService.checkIn(id)));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "Complete a checked-in booking")
    public ResponseEntity<ApiResponse<BookingResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Booking completed", bookingService.completeBooking(id)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelBookingRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled",
                bookingService.cancelBooking(id, req != null ? req.reason() : null)));
    }

    @PatchMapping("/{id}/override-dates")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Override booking dates (admin)")
    public ResponseEntity<ApiResponse<BookingResponse>> overrideDates(
            @PathVariable UUID id,
            @Valid @RequestBody OverrideDatesRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Dates updated",
                bookingService.overrideDates(id, req.checkIn(), req.checkOut())));
    }
}
