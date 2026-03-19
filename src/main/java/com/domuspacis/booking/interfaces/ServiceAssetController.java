package com.domuspacis.booking.interfaces;

import com.domuspacis.booking.application.ServiceAssetService;
import com.domuspacis.booking.interfaces.dto.BookingDtos.*;
import com.domuspacis.shared.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-assets")
@RequiredArgsConstructor
@Tag(name = "Service Assets", description = "Rooms, halls, gardens, retreat centers")
public class ServiceAssetController {

    private final ServiceAssetService service;

    @GetMapping
    @Operation(summary = "List all service assets")
    public ResponseEntity<ApiResponse<Page<ServiceAssetResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.listAll(pageable)));
    }

    @GetMapping("/available")
    @Operation(summary = "List all currently available assets")
    public ResponseEntity<ApiResponse<List<ServiceAssetResponse>>> available() {
        return ResponseEntity.ok(ApiResponse.success(service.listAvailable()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search assets by name")
    public ResponseEntity<ApiResponse<Page<ServiceAssetResponse>>> search(
            @RequestParam String q, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.search(q, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get asset by ID")
    public ResponseEntity<ApiResponse<ServiceAssetResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new service asset")
    public ResponseEntity<ApiResponse<ServiceAssetResponse>> create(
            @Valid @RequestBody CreateServiceAssetRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Asset created", service.create(req)));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle asset availability")
    public ResponseEntity<ApiResponse<ServiceAssetResponse>> setAvailability(
            @PathVariable UUID id, @RequestParam boolean available) {
        return ResponseEntity.ok(ApiResponse.success(service.setAvailability(id, available)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete service asset")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Asset deleted", null));
    }
}
