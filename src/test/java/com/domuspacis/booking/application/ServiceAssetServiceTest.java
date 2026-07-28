package com.domuspacis.booking.application;

import com.domuspacis.booking.domain.Room;
import com.domuspacis.booking.domain.ServiceAsset;
import com.domuspacis.booking.infrastructure.ServiceAssetRepository;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ServiceAssetService Unit Tests")
class ServiceAssetServiceTest {

    @Mock private ServiceAssetRepository serviceAssetRepository;

    @InjectMocks private ServiceAssetService serviceAssetService;

    private ServiceAsset testAsset;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();

        testAsset = Room.builder()
                .id(assetId)
                .name("Room 101")
                .assetType(com.domuspacis.booking.domain.ServiceAsset.AssetType.ROOM)
                .pricePerUnit(new BigDecimal("50000"))
                .pricingUnit(com.domuspacis.booking.domain.PricingUnit.PER_NIGHT)
                .capacity(2)
                .isAvailable(true)
                .roomNumber("101")
                .roomType(com.domuspacis.booking.domain.Room.RoomType.SINGLE)
                .floor(1)
                .build();
    }

    @Test
    @DisplayName("create - creates service asset successfully")
    void create_createsSuccessfully() {
        when(serviceAssetRepository.save(any(ServiceAsset.class))).thenReturn(testAsset);

        var request = new com.domuspacis.booking.interfaces.dto.BookingDtos.CreateServiceAssetRequest(
                "ROOM", "Room 101", "Standard room", 2,
                new BigDecimal("50000"), "PER_NIGHT",
                "101", "SINGLE", 1,
                null, null, null, null,
                null, null, null, null, null
        );

        var response = serviceAssetService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Room 101");
        verify(serviceAssetRepository).save(any(ServiceAsset.class));
    }

    @Test
    @DisplayName("getById - returns asset when exists")
    void getById_existingAsset_returnsAsset() {
        when(serviceAssetRepository.findById(assetId)).thenReturn(Optional.of(testAsset));

        var response = serviceAssetService.getById(assetId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(assetId);
    }

    @Test
    @DisplayName("getById - throws exception when asset not found")
    void getById_nonExistingAsset_throwsException() {
        when(serviceAssetRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceAssetService.getById(assetId));
    }

    @Test
    @DisplayName("listAll - returns paginated assets")
    void listAll_returnsPaginatedAssets() {
        Pageable pageable = mock(Pageable.class);
        Page<ServiceAsset> assetPage = new PageImpl<>(List.of(testAsset), pageable, 1);
        when(serviceAssetRepository.findAll(pageable)).thenReturn(assetPage);

        var response = serviceAssetService.listAll(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listAvailable - returns available assets")
    void listAvailable_returnsAvailableAssets() {
        when(serviceAssetRepository.findByIsAvailableTrue()).thenReturn(List.of(testAsset));

        var response = serviceAssetService.listAvailable();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
    }

    @Test
    @DisplayName("search - returns matching assets")
    void search_matchingQuery_returnsAssets() {
        Pageable pageable = mock(Pageable.class);
        Page<ServiceAsset> assetPage = new PageImpl<>(List.of(testAsset), pageable, 1);
        when(serviceAssetRepository.searchByName("Room", pageable)).thenReturn(assetPage);

        var response = serviceAssetService.search("Room", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("setAvailability - updates asset availability")
    void setAvailability_updatesSuccessfully() {
        when(serviceAssetRepository.findById(assetId)).thenReturn(Optional.of(testAsset));
        when(serviceAssetRepository.save(any(ServiceAsset.class))).thenReturn(testAsset);

        var response = serviceAssetService.setAvailability(assetId, false);

        assertThat(response).isNotNull();
        assertThat(testAsset.getIsAvailable()).isFalse();
        verify(serviceAssetRepository).save(testAsset);
    }

    @Test
    @DisplayName("setAvailability - throws exception when asset not found")
    void setAvailability_assetNotFound_throwsException() {
        when(serviceAssetRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> serviceAssetService.setAvailability(assetId, false));
    }

    @Test
    @DisplayName("delete - deletes asset successfully")
    void delete_existingAsset_deletesSuccessfully() {
        when(serviceAssetRepository.existsById(assetId)).thenReturn(true);
        doNothing().when(serviceAssetRepository).deleteById(assetId);

        serviceAssetService.delete(assetId);

        verify(serviceAssetRepository).deleteById(assetId);
    }

    @Test
    @DisplayName("delete - throws exception when asset not found")
    void delete_nonExistingAsset_throwsException() {
        when(serviceAssetRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceAssetService.delete(assetId));
    }
}