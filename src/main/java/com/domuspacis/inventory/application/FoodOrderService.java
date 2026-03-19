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
        log.info("Food order {} placed for customer {} total={}", saved.getId(), customerId, total);
        return saved;
    }

    public FoodOrder updateStatus(UUID orderId, FoodOrderStatus newStatus) {
        FoodOrder order = findById(orderId);
        if (order.getStatus() == FoodOrderStatus.CANCELLED)
            throw new BusinessRuleViolationException("Cancelled orders cannot be updated");
        order.setStatus(newStatus);

        // When delivered, record revenue transaction
        if (newStatus == FoodOrderStatus.DELIVERED) {
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

    private FoodOrder findById(UUID id) {
        return foodOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodOrder", id));
    }
}
