package com.domuspacis.tax.infrastructure;
import com.domuspacis.tax.domain.TaxRecord;
import com.domuspacis.tax.domain.TaxStatus;
import com.domuspacis.tax.domain.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface TaxRecordRepository extends JpaRepository<TaxRecord, UUID> {
    List<TaxRecord> findByPeriodYearAndPeriodMonth(int year, int month);
    List<TaxRecord> findByPeriodYear(int year);
    List<TaxRecord> findByTaxType(TaxType type);
    List<TaxRecord> findByStatus(TaxStatus status);
    @Query("SELECT COALESCE(SUM(t.taxDue),0) FROM TaxRecord t WHERE t.periodYear = :year AND t.status <> 'DRAFT'")
    java.math.BigDecimal sumTaxDueForYear(@Param("year") int year);
}
