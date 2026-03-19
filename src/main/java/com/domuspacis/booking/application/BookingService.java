package com.domuspacis.booking.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.booking.domain.*;
import com.domuspacis.booking.infrastructure.BookingRepository;
import com.domuspacis.booking.infrastructure.ServiceAssetRepository;
import com.domuspacis.booking.interfaces.dto.BookingDtos.*;
import com.domuspacis.customer.domain.Customer;
import com.domuspacis.customer.infrastructure.CustomerRepository;
import com.domuspacis.shared.exception.BookingConflictException;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository       bookingRepository;
    private final ServiceAssetRepository  serviceAssetRepository;
    private final CustomerRepository      customerRepository;
    private final AvailabilityService     availabilityService;
    private final ApplicationEventPublisher eventPublisher;

    public BookingResponse createBooking(CreateBookingRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", req.customerId()));
        ServiceAsset asset = serviceAssetRepository.findById(req.serviceAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceAsset", req.serviceAssetId()));

        if (!req.checkOutDate().isAfter(req.checkInDate()))
            throw new BusinessRuleViolationException("Check-out must be after check-in");

        if (!availabilityService.isAvailable(asset.getId(), req.checkInDate(), req.checkOutDate()))
            throw new BookingConflictException("Asset is not available for the requested dates: " + asset.getName());

        BigDecimal total = computeTotal(asset, req.checkInDate(), req.checkOutDate());

        Booking booking = Booking.builder()
                .customer(customer)
                .serviceAsset(asset)
                .checkInDate(req.checkInDate())
                .checkOutDate(req.checkOutDate())
                .numberOfGuests(req.numberOfGuests())
                .specialRequests(req.specialRequests())
                .totalAmount(total)
                .status(BookingStatus.PENDING)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking created: {} for customer: {}", booking.getId(), customer.getFullName());
        return toResponse(booking);
    }

    @Audited("CONFIRM_BOOKING")
    public BookingResponse confirmBooking(UUID bookingId) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING)
            throw new BusinessRuleViolationException("Only PENDING bookings can be confirmed");
        booking.setStatus(BookingStatus.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    @Audited("CHECK_IN_BOOKING")
    public BookingResponse checkIn(UUID bookingId) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new BusinessRuleViolationException("Only CONFIRMED bookings can be checked in");
        booking.setStatus(BookingStatus.CHECKED_IN);
        return toResponse(bookingRepository.save(booking));
    }

    @Audited("COMPLETE_BOOKING")
    public BookingResponse completeBooking(UUID bookingId) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_IN)
            throw new BusinessRuleViolationException("Only CHECKED_IN bookings can be completed");
        booking.setStatus(BookingStatus.COMPLETED);
        return toResponse(bookingRepository.save(booking));
    }

    @Audited("CANCEL_BOOKING")
    public BookingResponse cancelBooking(UUID bookingId, String reason) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() == BookingStatus.COMPLETED)
            throw new BusinessRuleViolationException("Completed bookings cannot be cancelled");
        booking.setStatus(BookingStatus.CANCELLED);
        if (reason != null) booking.setSpecialRequests(
                (booking.getSpecialRequests() != null ? booking.getSpecialRequests() + " | " : "")
                + "CANCELLATION REASON: " + reason);
        return toResponse(bookingRepository.save(booking));
    }

    @Audited("UPDATE_BOOKING_STATUS")
    public BookingResponse updateStatus(UUID bookingId, BookingStatus newStatus) {
        Booking booking = findById(bookingId);
        booking.setStatus(newStatus);
        return toResponse(bookingRepository.save(booking));
    }

    @Audited("OVERRIDE_BOOKING_DATES")
    public BookingResponse overrideDates(UUID bookingId, LocalDate newCheckIn, LocalDate newCheckOut) {
        Booking booking = findById(bookingId);
        if (!availabilityService.isAvailableExcluding(
                booking.getServiceAsset().getId(), newCheckIn, newCheckOut, bookingId))
            throw new BookingConflictException("Asset is not available for the new dates");
        booking.setCheckInDate(newCheckIn);
        booking.setCheckOutDate(newCheckOut);
        booking.setTotalAmount(computeTotal(booking.getServiceAsset(), newCheckIn, newCheckOut));
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listAll(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listByCustomer(UUID customerId, Pageable pageable) {
        return bookingRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listByStatus(BookingStatus status, Pageable pageable) {
        return bookingRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean checkAvailability(UUID assetId, LocalDate checkIn, LocalDate checkOut) {
        return availabilityService.isAvailable(assetId, checkIn, checkOut);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Booking findById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private BigDecimal computeTotal(ServiceAsset asset, LocalDate checkIn, LocalDate checkOut) {
        long units = switch (asset.getPricingUnit()) {
            case PER_NIGHT, PER_DAY -> ChronoUnit.DAYS.between(checkIn, checkOut);
            case PER_HOUR            -> ChronoUnit.DAYS.between(checkIn, checkOut) * 24L;
            case PER_EVENT           -> 1L;
        };
        return asset.getPricePerUnit().multiply(BigDecimal.valueOf(Math.max(units, 1)));
    }

    public BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getCustomer().getId(),
                b.getCustomer().getFullName(),
                b.getServiceAsset().getId(),
                b.getServiceAsset().getName(),
                b.getCheckInDate(), b.getCheckOutDate(),
                b.getNumberOfGuests(), b.getStatus().name(),
                b.getSpecialRequests(), b.getTotalAmount(),
                b.getCreatedAt());
    }
}
