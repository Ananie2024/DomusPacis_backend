package com.domuspacis.finance.infrastructure;
import com.domuspacis.finance.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBookingId(UUID bookingId);
    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);
    @Query("SELECT p FROM Payment p WHERE p.paidAt BETWEEN :from AND :to")
    List<Payment> findByPaidAtBetween(Instant from, Instant to);
    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p WHERE p.status = 'PAID' AND p.paidAt BETWEEN :from AND :to")
    java.math.BigDecimal sumPaidBetween(Instant from, Instant to);
}
