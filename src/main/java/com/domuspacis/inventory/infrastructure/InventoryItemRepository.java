package com.domuspacis.inventory.infrastructure;
import com.domuspacis.inventory.domain.InventoryItem;
import com.domuspacis.inventory.domain.ItemCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Page<InventoryItem> findByCategory(ItemCategory category, Pageable pageable);
    @Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.reorderLevel")
    List<InventoryItem> findLowStock();
    @Query("SELECT i FROM InventoryItem i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<InventoryItem> searchByName(@Param("q") String q, Pageable pageable);
}
