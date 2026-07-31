package com.domuspacis.staff.infrastructure;

import com.domuspacis.staff.domain.PayeTaxBracket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayeTaxBracketRepository extends JpaRepository<PayeTaxBracket, UUID> {

    /**
     * Finds all active PAYE tax brackets effective on the given date,
     * ordered by lower_bound ascending.
     */
    @Query("SELECT b FROM PayeTaxBracket b WHERE b.isActive = true "
         + "AND b.effectiveFrom <= :date "
         + "AND (b.effectiveTo IS NULL OR b.effectiveTo >= :date) "
         + "ORDER BY b.lowerBound ASC")
    List<PayeTaxBracket> findActiveBrackets(@Param("date") LocalDate date);
}