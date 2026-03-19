package com.domuspacis.booking.application;

import com.domuspacis.booking.domain.ServiceAsset;
import com.domuspacis.booking.infrastructure.BookingRepository;
import com.domuspacis.booking.infrastructure.ServiceAssetRepository;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final BookingRepository bookingRepository;
    private final ServiceAssetRepository serviceAssetRepository;

    public boolean isAvailable(UUID assetId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) return false;
        ServiceAsset asset = serviceAssetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceAsset", assetId));
        if (!Boolean.TRUE.equals(asset.getIsAvailable())) return false;
        return bookingRepository.findConflicting(assetId, checkIn, checkOut).isEmpty();
    }

    public boolean isAvailableExcluding(UUID assetId, LocalDate checkIn, LocalDate checkOut, UUID excludeBookingId) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) return false;
        return bookingRepository.findConflictingExcluding(assetId, checkIn, checkOut, excludeBookingId).isEmpty();
    }
}
