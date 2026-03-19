package com.domuspacis.inventory.infrastructure;
import com.domuspacis.inventory.domain.FoodOrder;
import com.domuspacis.inventory.domain.FoodOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, UUID> {
    Page<FoodOrder> findByCustomerId(UUID customerId, Pageable pageable);
    Page<FoodOrder> findByStatus(FoodOrderStatus status, Pageable pageable);
    Page<FoodOrder> findByBookingId(UUID bookingId, Pageable pageable);
}
