package com.domuspacis.booking.interfaces.dto;

import com.domuspacis.booking.domain.BookingStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class BookingDtos {
    private BookingDtos() {}

    public record CreateBookingRequest(
        @NotNull UUID customerId,
        @NotNull UUID serviceAssetId,
        @NotNull LocalDate checkInDate,
        @NotNull LocalDate checkOutDate,
        @Min(1) Integer numberOfGuests,
        String specialRequests
    ) {}

    public record BookingResponse(
        UUID id,
        UUID customerId,
        String customerName,
        UUID serviceAssetId,
        String serviceAssetName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer numberOfGuests,
        String status,
        String specialRequests,
        BigDecimal totalAmount,
        Instant createdAt
    ) {}

    public record AvailabilityRequest(
        @NotNull UUID assetId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut
    ) {}

    public record AvailabilityResponse(
        UUID assetId,
        LocalDate checkIn,
        LocalDate checkOut,
        boolean available
    ) {}

    public record ServiceAssetResponse(
        UUID id,
        String assetType,
        String name,
        String description,
        Integer capacity,
        BigDecimal pricePerUnit,
        String pricingUnit,
        Boolean isAvailable
    ) {}

    public record CreateServiceAssetRequest(
        @NotBlank String assetType,
        @NotBlank String name,
        String description,
        Integer capacity,
        @NotNull BigDecimal pricePerUnit,
        @NotBlank String pricingUnit,
        // Room specific
        String roomNumber, String roomType, Integer floor,
        // Hall specific
        String hallCode, Boolean projectorAvailable, Boolean audioSystemAvailable, String seatingLayout,
        // Wedding
        Boolean isIndoor, Boolean hasStage,
        // Retreat
        Integer numberOfBeds, Boolean includesChapel, Boolean includesCatering
    ) {}

    public record UpdateBookingStatusRequest(@NotNull BookingStatus status) {}
    public record CancelBookingRequest(String reason) {}
    public record OverrideDatesRequest(@NotNull LocalDate checkIn, @NotNull LocalDate checkOut) {}
}
