package com.domuspacis.inventory.interfaces;

import com.domuspacis.inventory.application.FoodOrderService;
import com.domuspacis.inventory.application.InventoryService;
import com.domuspacis.inventory.domain.*;
import com.domuspacis.shared.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock, suppliers, menu items and food orders")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService  inventoryService;
    private final FoodOrderService  foodOrderService;

    // ── Suppliers ─────────────────────────────────────────────────────────────

    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Create supplier")
    public ResponseEntity<ApiResponse<SupplierDto>> createSupplier(
            @Valid @RequestBody CreateSupplierRequest req) {
        Supplier s = inventoryService.createSupplier(req.name(), req.contactPerson(),
                req.phone(), req.email(), req.address(), req.taxIdentificationNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supplier created", toSupplierDto(s)));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "List active suppliers")
    public ResponseEntity<ApiResponse<Page<SupplierDto>>> listSuppliers(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.listSuppliers(pageable).map(this::toSupplierDto)));
    }

    @GetMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<SupplierDto>> getSupplier(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toSupplierDto(inventoryService.getSupplierById(id))));
    }

    @DeleteMapping("/suppliers/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Deactivate supplier")
    public ResponseEntity<ApiResponse<SupplierDto>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toSupplierDto(inventoryService.deactivateSupplier(id))));
    }

    // ── Inventory Items ───────────────────────────────────────────────────────

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Create inventory item")
    public ResponseEntity<ApiResponse<InventoryItemDto>> createItem(
            @Valid @RequestBody CreateItemRequest req) {
        InventoryItem item = inventoryService.createItem(req.name(), req.category(),
                req.unit(), req.reorderLevel(), req.unitCost(), req.supplierId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item created", toItemDto(item)));
    }

    @GetMapping("/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all inventory items")
    public ResponseEntity<ApiResponse<Page<InventoryItemDto>>> listItems(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.listItems(pageable).map(this::toItemDto)));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InventoryItemDto>> getItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toItemDto(inventoryService.getItemById(id))));
    }

    @GetMapping("/items/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "List items at or below reorder level")
    public ResponseEntity<ApiResponse<List<InventoryItemDto>>> lowStock() {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.listLowStock().stream().map(this::toItemDto).toList()));
    }

    // ── Stock Movements ───────────────────────────────────────────────────────

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "Record a stock movement")
    public ResponseEntity<ApiResponse<MovementDto>> recordMovement(
            @Valid @RequestBody RecordMovementRequest req) {
        StockMovement mv = inventoryService.recordMovement(
                req.itemId(), req.movementType(), req.quantity(), req.referenceNote(), req.recordedById());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Movement recorded", toMovementDto(mv)));
    }

    @GetMapping("/movements/item/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "List movements for an item")
    public ResponseEntity<ApiResponse<Page<MovementDto>>> movements(
            @PathVariable UUID itemId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.listMovementsByItem(itemId, pageable).map(this::toMovementDto)));
    }

    // ── Menu Items ────────────────────────────────────────────────────────────

    @PostMapping("/menu-items")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Create a menu item")
    public ResponseEntity<ApiResponse<MenuItemDto>> createMenuItem(
            @Valid @RequestBody CreateMenuItemRequest req) {
        MenuItem item = inventoryService.createMenuItem(req.name(), req.category(),
                req.description(), req.unitPrice(), req.ingredientIds());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created", toMenuItemDto(item)));
    }

    @GetMapping("/menu-items")
    @Operation(summary = "List all menu items")
    public ResponseEntity<ApiResponse<Page<MenuItemDto>>> listMenu(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.listMenuItems(pageable).map(this::toMenuItemDto)));
    }

    @GetMapping("/menu-items/available")
    @Operation(summary = "List available menu items")
    public ResponseEntity<ApiResponse<List<MenuItemDto>>> availableMenu() {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.listAvailableMenuItems().stream().map(this::toMenuItemDto).toList()));
    }

    @GetMapping("/menu-items/{id}")
    public ResponseEntity<ApiResponse<MenuItemDto>> getMenuItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toMenuItemDto(inventoryService.getMenuItemById(id))));
    }

    @PatchMapping("/menu-items/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Toggle menu item availability")
    public ResponseEntity<ApiResponse<MenuItemDto>> toggleAvailability(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toMenuItemDto(inventoryService.toggleMenuItemAvailability(id))));
    }

    // ── Food Orders ───────────────────────────────────────────────────────────

    @PostMapping("/food-orders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Place a food order")
    public ResponseEntity<ApiResponse<FoodOrderDto>> placeOrder(@Valid @RequestBody PlaceFoodOrderRequest req) {
        FoodOrder order = foodOrderService.placeOrder(req.customerId(), req.bookingId(),
                req.itemQuantities(), req.deliveryLocation());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed", toFoodOrderDto(order)));
    }

    @GetMapping("/food-orders")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "List all food orders")
    public ResponseEntity<ApiResponse<Page<FoodOrderDto>>> listOrders(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(foodOrderService.listAll(pageable).map(this::toFoodOrderDto)));
    }

    @GetMapping("/food-orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FoodOrderDto>> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toFoodOrderDto(foodOrderService.getById(id))));
    }

    @PatchMapping("/food-orders/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "Update food order status")
    public ResponseEntity<ApiResponse<FoodOrderDto>> updateStatus(
            @PathVariable UUID id, @RequestParam FoodOrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(toFoodOrderDto(foodOrderService.updateStatus(id, status))));
    }

    @PatchMapping("/food-orders/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a food order")
    public ResponseEntity<ApiResponse<FoodOrderDto>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toFoodOrderDto(foodOrderService.cancelOrder(id))));
    }

    // ── DTOs + mappers ────────────────────────────────────────────────────────

    record CreateSupplierRequest(
        @NotBlank String name, String contactPerson, String phone,
        String email, String address, String taxIdentificationNumber) {}
    record SupplierDto(UUID id, String name, String contactPerson, String phone,
                        String email, Boolean isActive) {}

    record CreateItemRequest(
        @NotBlank String name, @NotNull ItemCategory category, @NotBlank String unit,
        BigDecimal reorderLevel, BigDecimal unitCost, UUID supplierId) {}
    record InventoryItemDto(UUID id, String name, String category, String unit,
                             BigDecimal currentStock, BigDecimal reorderLevel,
                             BigDecimal unitCost, String supplierName, boolean lowStock) {}

    record RecordMovementRequest(
        @NotNull UUID itemId, @NotNull MovementType movementType,
        @NotNull @Positive BigDecimal quantity,
        String referenceNote, UUID recordedById) {}
    record MovementDto(UUID id, UUID itemId, String itemName, String movementType,
                        BigDecimal quantity, java.time.LocalDate movementDate, String referenceNote) {}

    record CreateMenuItemRequest(
        @NotBlank String name, @NotNull MenuItem.MenuCategory category,
        String description, @NotNull @Positive BigDecimal unitPrice,
        List<UUID> ingredientIds) {}
    record MenuItemDto(UUID id, String name, String category, String description,
                        BigDecimal unitPrice, Boolean isAvailable) {}

    record PlaceFoodOrderRequest(
        @NotNull UUID customerId, UUID bookingId,
        @NotEmpty Map<UUID, Integer> itemQuantities,
        String deliveryLocation) {}
    record FoodOrderDto(UUID id, UUID customerId, UUID bookingId, String status,
                         BigDecimal totalAmount, String deliveryLocation, Instant orderedAt,
                         int itemCount) {}

    private SupplierDto toSupplierDto(Supplier s) {
        return new SupplierDto(s.getId(), s.getName(), s.getContactPerson(), s.getPhone(), s.getEmail(), s.getIsActive());
    }
    private InventoryItemDto toItemDto(InventoryItem i) {
        boolean low = i.getCurrentStock().compareTo(i.getReorderLevel()) <= 0;
        String sName = i.getSupplier() != null ? i.getSupplier().getName() : null;
        return new InventoryItemDto(i.getId(), i.getName(), i.getCategory().name(), i.getUnit(),
                i.getCurrentStock(), i.getReorderLevel(), i.getUnitCost(), sName, low);
    }
    private MovementDto toMovementDto(StockMovement m) {
        return new MovementDto(m.getId(), m.getItem().getId(), m.getItem().getName(),
                m.getMovementType().name(), m.getQuantity(), m.getMovementDate(), m.getReferenceNote());
    }
    private MenuItemDto toMenuItemDto(MenuItem m) {
        return new MenuItemDto(m.getId(), m.getName(), m.getCategory().name(),
                m.getDescription(), m.getUnitPrice(), m.getIsAvailable());
    }
    private FoodOrderDto toFoodOrderDto(FoodOrder o) {
        UUID bookingId = o.getBooking() != null ? o.getBooking().getId() : null;
        return new FoodOrderDto(o.getId(), o.getCustomer().getId(), bookingId,
                o.getStatus().name(), o.getTotalAmount(),
                o.getDeliveryLocation(), o.getOrderedAt(), o.getItems().size());
    }
}
