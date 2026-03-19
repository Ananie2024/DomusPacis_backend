package com.domuspacis.finance.infrastructure;
import com.domuspacis.finance.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByBookingId(UUID bookingId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Page<Invoice> findByStatus(Invoice.InvoiceStatus status, Pageable pageable);
    boolean existsByInvoiceNumber(String invoiceNumber);
}
