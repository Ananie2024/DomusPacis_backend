package com.domuspacis.staff.infrastructure;
import com.domuspacis.staff.domain.PayrollRecord;
import com.domuspacis.staff.domain.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface PayrollRepository extends JpaRepository<PayrollRecord, UUID> {
    Optional<PayrollRecord> findByEmployeeIdAndPeriodYearAndPeriodMonth(UUID empId, int year, int month);
    List<PayrollRecord> findByPeriodYearAndPeriodMonth(int year, int month);
    Page<PayrollRecord> findByStatus(PayrollStatus status, Pageable pageable);
    @Query("SELECT COALESCE(SUM(p.netSalary),0) FROM PayrollRecord p WHERE p.periodYear = :year AND p.periodMonth = :month AND p.status = 'PAID'")
    BigDecimal totalNetPaidForMonth(@Param("year") int year, @Param("month") int month);
}
