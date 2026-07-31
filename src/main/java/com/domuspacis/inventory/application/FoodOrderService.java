package com.domuspacis.inventory.application;

import com.domuspacis.booking.infrastructure.BookingRepository;
import com.domuspacis.customer.infrastructure.CustomerRepository;
import com.domuspacis.finance.domain.RevenueSourceType;
import com.domuspacis.finance.domain.RevenueTransaction;
import com.domuspacis.finance.infrastructure.RevenueTransactionRepository;
import com.domuspacis.inventory.domain.*;
import com.domuspacis.inventory.infrastructure.*;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FoodOrderService {

    private final FoodOrderRepository          foodOrderRepository;
    private final MenuItemRepository           menuItemRepository;
    private final CustomerRepository           customerRepository;
    private final BookingRepository            bookingRepository;
    private final RevenueTransactionRepository revenueTransactionRepository;
    private final InventoryService             inventoryService;
    private final InventoryItemRepository      inventoryItemRepository;

    public FoodOrder placeOrder(UUID customerId, UUID bookingId,
                                 Map<UUID, Integer> itemQuantities,
                                 String deliveryLocation) {
        var customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        var booking = bookingId != null
                ? bookingRepository.findById(bookingId).orElse(null) : null;

        if (itemQuantities == null || itemQuantities.isEmpty())
            throw new BusinessRuleViolationException("Order must contain at least one item");

        FoodOrder order = FoodOrder.builder()
                .customer(customer).booking(booking)
                .deliveryLocation(deliveryLocation)
                .status(FoodOrderStatus.PENDING)
                .build();

        List<FoodOrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<UUID, Integer> entry : itemQuantities.entrySet()) {
            MenuItem menuItem = menuItemRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", entry.getKey()));
            if (!Boolean.TRUE.equals(menuItem.getIsAvailable()))
                throw new BusinessRuleViolationException("Menu item not available: " + menuItem.getName());

            int qty = entry.getValue();
            BigDecimal subtotal = menuItem.getUnitPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(subtotal);

            FoodOrderItem item = FoodOrderItem.builder()
                    .foodOrder(order).menuItem(menuItem)
                    .quantity(qty).unitPrice(menuItem.getUnitPrice()).subtotal(subtotal)
                    .build();
            orderItems.add(item);
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);
        FoodOrder saved = foodOrderRepository.save(order);
        // Deduct inventory for each item's ingredients
        deductInventoryForOrder(orderItems);
        log.info("Food order {} placed for customer {} total={}", saved.getId(), customerId, total);
        return saved;
    }

    public FoodOrder updateStatus(UUID orderId, FoodOrderStatus newStatus) {
        FoodOrder order = findById(orderId);
        if (order.getStatus() == FoodOrderStatus.CANCELLED)
            throw new BusinessRuleViolationException("Cancelled orders cannot be updated");
        // Idempotent: no-op if already in the target status
        if (order.getStatus() == newStatus) {
            log.info("Food order {} already has status {}, no change", orderId, newStatus);
            return order;
        }

        order.setStatus(newStatus);

        // When preparing, deduct inventory (idempotent — skip if already deducted)
        if (newStatus == FoodOrderStatus.PREPARING) {
            if (!order.getItems().isEmpty() && order.getItems().get(0).getMenuItem().getIngredients().isEmpty()) {
                log.info("No ingredients to deduct for food order {}, skipping", order.getId());
            } else {
                deductInventoryForOrder(order.getItems());
            }
        }

        // When delivered, record revenue transaction (idempotent)
        if (newStatus == FoodOrderStatus.DELIVERED) {
            if (revenueTransactionRepository.findBySourceTypeAndSourceId(
                    RevenueSourceType.FOOD_SERVICE, order.getId()).isPresent()) {
                log.info("Revenue transaction already exists for food order {}, skipping", order.getId());
            } else {
                RevenueTransaction rt = RevenueTransaction.builder()
                        .sourceType(RevenueSourceType.FOOD_SERVICE)
                        .sourceId(order.getId())
                        .amount(order.getTotalAmount())
                        .currency("RWF")
                        .transactionDate(LocalDate.now())
                        .description("Food order delivered to: " + order.getDeliveryLocation())
                        .build();
                revenueTransactionRepository.save(rt);
            }
        }
        return foodOrderRepository.save(order);
    }

    public FoodOrder cancelOrder(UUID orderId) {
        FoodOrder order = findById(orderId);
        if (order.getStatus() == FoodOrderStatus.DELIVERED)
            throw new BusinessRuleViolationException("Delivered orders cannot be cancelled");
        order.setStatus(FoodOrderStatus.CANCELLED);
        return foodOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public FoodOrder getById(UUID id) { return findById(id); }

    @Transactional(readOnly = true)
    public Page<FoodOrder> listAll(Pageable pageable) { return foodOrderRepository.findAll(pageable); }

    @Transactional(readOnly = true)
    public Page<FoodOrder> listByCustomer(UUID customerId, Pageable pageable) {
        return foodOrderRepository.findByCustomerId(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FoodOrder> listByStatus(FoodOrderStatus status, Pageable pageable) {
        return foodOrderRepository.findByStatus(status, pageable);
    }

    private void deductInventoryForOrder(List<FoodOrderItem> orderItems) {
        for (FoodOrderItem item : orderItems) {
            MenuItem menuItem = item.getMenuItem();
            List<InventoryItem> ingredients = menuItem.getIngredients();
            if (ingredients == null || ingredients.isEmpty()) continue;

            int qty = item.getQuantity();
            for (InventoryItem ingredient : ingredients) {
                try {
                    BigDecimal consumptionQty = BigDecimal.valueOf(qty);
                    // Check if sufficient stock exists
                    InventoryItem freshItem = inventoryItemRepository.findByIdWithLock(ingredient.getId())
                            .orElse(null);
                    if (freshItem == null) {
                        log.warn("Ingredient {} not found for deduction, skipping", ingredient.getId());
                        continue;
                    }
                    if (freshItem.getCurrentStock().compareTo(consumptionQty) < 0) {
                        log.warn("Insufficient stock for ingredient {}: have {}, need {}",
                                freshItem.getName(), freshItem.getCurrentStock(), consumptionQty);
                        // Still deduct what we can (or skip entirely — let's skip to avoid negative stock)
                        continue;
                    }
                    // Use CONSUMPTION movement type — recordedBy is null (system-triggered)
                    inventoryService.recordMovement(
                            ingredient.getId(),
                            MovementType.CONSUMPTION,
                            consumptionQty,
                            "Auto-deducted for food order item: " + menuItem.getName() + " x" + qty,
                            null
                    );
                } catch (Exception e) {
                    log.error("Failed to deduct ingredient {} for menu item {}: {}",
                            ingredient.getId(), menuItem.getName(), e.getMessage());
                }
            }
        }
    }

    private FoodOrder findById(UUID id) {
        return foodOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodOrder", id));
    }
}
