package com.domuspacis.inventory.infrastructure;
import com.domuspacis.inventory.domain.MovementType;
import com.domuspacis.inventory.domain.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    Page<StockMovement> findByItemId(UUID itemId, Pageable pageable);
    List<StockMovement> findByMovementDateBetween(LocalDate from, LocalDate to);
    List<StockMovement> findByMovementType(MovementType type);
}
