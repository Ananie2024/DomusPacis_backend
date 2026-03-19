package com.domuspacis.tax.infrastructure;
import com.domuspacis.tax.domain.TaxRuleConfig;
import com.domuspacis.tax.domain.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface TaxRuleConfigRepository extends JpaRepository<TaxRuleConfig, UUID> {
    @Query("SELECT t FROM TaxRuleConfig t WHERE t.taxType = :type AND t.isActive = true AND t.effectiveFrom <= :date AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date) ORDER BY t.effectiveFrom DESC")
    Optional<TaxRuleConfig> findActiveRule(@Param("type") TaxType type, @Param("date") LocalDate date);
}
