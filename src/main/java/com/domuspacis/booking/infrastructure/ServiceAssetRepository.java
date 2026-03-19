package com.domuspacis.booking.infrastructure;
import com.domuspacis.booking.domain.ServiceAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface ServiceAssetRepository extends JpaRepository<ServiceAsset, UUID> {
    List<ServiceAsset> findByIsAvailableTrue();
    @Query("SELECT s FROM ServiceAsset s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<ServiceAsset> searchByName(@Param("q") String query, Pageable pageable);
}
