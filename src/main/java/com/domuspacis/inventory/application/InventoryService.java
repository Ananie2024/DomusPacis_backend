package com.domuspacis.inventory.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.inventory.domain.*;
import com.domuspacis.inventory.infrastructure.*;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import com.domuspacis.staff.infrastructure.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final StockMovementRepository movementRepository;
    private final SupplierRepository      supplierRepository;
    private final MenuItemRepository      menuItemRepository;
    private final EmployeeRepository      employeeRepository;

    // ── Supplier ──────────────────────────────────────────────────────────────

    public Supplier createSupplier(String name, String contactPerson, String phone,
                                    String email, String address, String tin) {
        return supplierRepository.save(Supplier.builder()
                .name(name).contactPerson(contactPerson).phone(phone)
                .email(email).address(address).taxIdentificationNumber(tin).build());
    }

    @Audited("DEACTIVATE_SUPPLIER")
    public Supplier deactivateSupplier(UUID supplierId) {
        Supplier s = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
        s.setIsActive(false);
        return supplierRepository.save(s);
    }

    @Transactional(readOnly = true)
    public Page<Supplier> listSuppliers(Pageable pageable) {
        return supplierRepository.findByIsActiveTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Supplier getSupplierById(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    // ── InventoryItem ─────────────────────────────────────────────────────────

    public InventoryItem createItem(String name, ItemCategory category, String unit,
                                     BigDecimal reorderLevel, BigDecimal unitCost, UUID supplierId) {
        Supplier supplier = supplierId != null
                ? supplierRepository.findById(supplierId).orElse(null) : null;
        return itemRepository.save(InventoryItem.builder()
                .name(name).category(category).unit(unit)
                .currentStock(BigDecimal.ZERO).reorderLevel(reorderLevel)
                .unitCost(unitCost).supplier(supplier).build());
    }

    @Transactional(readOnly = true)
    public InventoryItem getItemById(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", id));
    }

    @Transactional(readOnly = true)
    public Page<InventoryItem> listItems(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItem> listByCategory(ItemCategory category, Pageable pageable) {
        return itemRepository.findByCategory(category, pageable);
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> listLowStock() {
        return itemRepository.findLowStock();
    }

    // ── Stock Movements ───────────────────────────────────────────────────────

    @Audited("APPROVE_STOCK_MOVEMENT")
    public StockMovement recordMovement(UUID itemId, MovementType type,
                                         BigDecimal quantity, String note, UUID recordedById) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", itemId));

        if (quantity.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessRuleViolationException("Quantity must be positive");

        // Update stock level
        BigDecimal newStock = switch (type) {
            case RECEIPT                    -> item.getCurrentStock().add(quantity);
            case CONSUMPTION, WASTE         -> item.getCurrentStock().subtract(quantity);
            case ADJUSTMENT                 -> quantity; // absolute adjustment
        };

        if (newStock.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessRuleViolationException(
                    "Insufficient stock for " + item.getName() + ". Available: " + item.getCurrentStock());

        item.setCurrentStock(newStock);
        itemRepository.save(item);

        var recorder = recordedById != null
                ? employeeRepository.findById(recordedById).orElse(null) : null;

        StockMovement mv = StockMovement.builder()
                .item(item).movementType(type).quantity(quantity)
                .movementDate(LocalDate.now()).referenceNote(note).recordedBy(recorder)
                .build();

        StockMovement saved = movementRepository.save(mv);

        // Low-stock alert
        if (newStock.compareTo(item.getReorderLevel()) <= 0)
            log.warn("LOW STOCK ALERT: {} — current={} reorder={}", item.getName(), newStock, item.getReorderLevel());

        return saved;
    }

    @Audited("ADJUST_STOCK_LEVEL")
    public StockMovement adjustStock(UUID itemId, BigDecimal newAbsoluteLevel,
                                      String reason, UUID recordedById) {
        return recordMovement(itemId, MovementType.ADJUSTMENT, newAbsoluteLevel, reason, recordedById);
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> listMovementsByItem(UUID itemId, Pageable pageable) {
        return movementRepository.findByItemId(itemId, pageable);
    }

    // ── MenuItem ──────────────────────────────────────────────────────────────

    public MenuItem createMenuItem(String name, MenuItem.MenuCategory category,
                                    String description, BigDecimal unitPrice,
                                    List<UUID> ingredientIds) {
        List<InventoryItem> ingredients = ingredientIds != null
                ? ingredientIds.stream()
                        .map(id -> itemRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", id)))
                        .toList()
                : List.of();

        return menuItemRepository.save(MenuItem.builder()
                .name(name).category(category).description(description)
                .unitPrice(unitPrice).isAvailable(true).ingredients(ingredients).build());
    }

    public MenuItem toggleMenuItemAvailability(UUID id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
        item.setIsAvailable(!item.getIsAvailable());
        return menuItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<MenuItem> listAvailableMenuItems() { return menuItemRepository.findByIsAvailableTrue(); }

    @Transactional(readOnly = true)
    public MenuItem getMenuItemById(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
    }

    @Transactional(readOnly = true)
    public Page<MenuItem> listMenuItems(Pageable pageable) { return menuItemRepository.findAll(pageable); }
}
