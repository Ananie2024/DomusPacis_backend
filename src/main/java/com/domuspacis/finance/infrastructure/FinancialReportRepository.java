package com.domuspacis.finance.infrastructure;
import com.domuspacis.finance.domain.FinancialReport;
import com.domuspacis.finance.domain.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, UUID> {
    List<FinancialReport> findByReportTypeOrderByPeriodDesc(ReportType type);
    Page<FinancialReport> findAllByOrderByGeneratedAtDesc(Pageable pageable);
}
