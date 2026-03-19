package com.domuspacis.booking.application;

import com.domuspacis.booking.domain.*;
import com.domuspacis.booking.infrastructure.ServiceAssetRepository;
import com.domuspacis.booking.interfaces.dto.BookingDtos.*;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceAssetService {

    private final ServiceAssetRepository repo;

    public ServiceAssetResponse create(CreateServiceAssetRequest req) {
        ServiceAsset asset = buildAsset(req);
        return toResponse(repo.save(asset));
    }

    @Transactional(readOnly = true)
    public ServiceAssetResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<ServiceAssetResponse> listAll(Pageable pageable) {
        return repo.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ServiceAssetResponse> listAvailable() {
        return repo.findByIsAvailableTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ServiceAssetResponse> search(String q, Pageable pageable) {
        return repo.searchByName(q, pageable).map(this::toResponse);
    }

    public ServiceAssetResponse setAvailability(UUID id, boolean available) {
        ServiceAsset asset = findById(id);
        asset.setIsAvailable(available);
        return toResponse(repo.save(asset));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("ServiceAsset", id);
        repo.deleteById(id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ServiceAsset findById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("ServiceAsset", id));
    }

    private ServiceAsset buildAsset(CreateServiceAssetRequest r) {
        return switch (r.assetType().toUpperCase()) {
            case "ROOM" -> Room.builder()
                    .name(r.name()).description(r.description()).capacity(r.capacity())
                    .pricePerUnit(r.pricePerUnit()).pricingUnit(PricingUnit.valueOf(r.pricingUnit()))
                    .roomNumber(r.roomNumber())
                    .roomType(r.roomType() != null ? Room.RoomType.valueOf(r.roomType()) : null)
                    .floor(r.floor()).build();
            case "CONFERENCE_HALL", "CONF_HALL" -> ConferenceHall.builder()
                    .name(r.name()).description(r.description()).capacity(r.capacity())
                    .pricePerUnit(r.pricePerUnit()).pricingUnit(PricingUnit.valueOf(r.pricingUnit()))
                    .hallCode(r.hallCode()).projectorAvailable(r.projectorAvailable())
                    .audioSystemAvailable(r.audioSystemAvailable())
                    .maxSeatingLayout(r.seatingLayout() != null
                            ? ConferenceHall.SeatingLayout.valueOf(r.seatingLayout()) : null)
                    .build();
            case "WEDDING_GARDEN" -> WeddingGarden.builder()
                    .name(r.name()).description(r.description()).capacity(r.capacity())
                    .pricePerUnit(r.pricePerUnit()).pricingUnit(PricingUnit.valueOf(r.pricingUnit()))
                    .isIndoor(r.isIndoor()).hasStage(r.hasStage()).build();
            case "RETREAT" -> RetreatCenter.builder()
                    .name(r.name()).description(r.description()).capacity(r.capacity())
                    .pricePerUnit(r.pricePerUnit()).pricingUnit(PricingUnit.valueOf(r.pricingUnit()))
                    .numberOfBeds(r.numberOfBeds()).includesChapel(r.includesChapel())
                    .includesCatering(r.includesCatering()).build();
            default -> throw new BusinessRuleViolationException("Unknown asset type: " + r.assetType());
        };
    }

    public ServiceAssetResponse toResponse(ServiceAsset s) {
        String type = s.getClass().getAnnotation(
                jakarta.persistence.DiscriminatorValue.class) != null
                ? s.getClass().getAnnotation(
                        jakarta.persistence.DiscriminatorValue.class).value()
                : s.getClass().getSimpleName();
        return new ServiceAssetResponse(s.getId(), type, s.getName(), s.getDescription(),
                s.getCapacity(), s.getPricePerUnit(),
                s.getPricingUnit() != null ? s.getPricingUnit().name() : null,
                s.getIsAvailable());
    }
}
