package com.domuspacis.finance.infrastructure;
import com.domuspacis.finance.domain.RevenueSourceType;
import com.domuspacis.finance.domain.RevenueTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Repository
public interface RevenueTransactionRepository extends JpaRepository<RevenueTransaction, UUID> {
    List<RevenueTransaction> findByTransactionDateBetween(LocalDate from, LocalDate to);
    Page<RevenueTransaction> findBySourceType(RevenueSourceType type, Pageable pageable);
    @Query("SELECT COALESCE(SUM(r.amount),0) FROM RevenueTransaction r WHERE r.transactionDate BETWEEN :from AND :to")
    BigDecimal sumByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
    @Query("SELECT r.sourceType, COALESCE(SUM(r.amount),0) FROM RevenueTransaction r WHERE r.transactionDate BETWEEN :from AND :to GROUP BY r.sourceType")
    List<Object[]> revenueBySourceType(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
