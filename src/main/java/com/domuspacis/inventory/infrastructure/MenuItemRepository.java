package com.domuspacis.inventory.infrastructure;
import com.domuspacis.inventory.domain.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    List<MenuItem> findByIsAvailableTrue();
    Page<MenuItem> findByCategory(MenuItem.MenuCategory category, Pageable pageable);
}
