package com.domuspacis.finance.application;

import com.domuspacis.booking.domain.Booking;
import com.domuspacis.booking.domain.Room;
import com.domuspacis.booking.domain.ServiceAsset;
import com.domuspacis.finance.domain.Invoice;
import com.domuspacis.finance.infrastructure.InvoiceRepository;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@DisplayName("InvoiceService Unit Tests")
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentService paymentService;
    @Mock private com.domuspacis.booking.infrastructure.BookingRepository bookingRepository;

    @InjectMocks private InvoiceService invoiceService;

    private Invoice testInvoice;
    private Booking testBooking;
    private UUID invoiceId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        ServiceAsset testAsset = Room.builder()
                .id(UUID.randomUUID())
                .name("Room 101")
                .pricePerUnit(new BigDecimal("50000"))
                .roomNumber("101")
                .roomType(com.domuspacis.booking.domain.Room.RoomType.SINGLE)
                .build();

        testBooking = Booking.builder()
                .serviceAsset(testAsset)
                .totalAmount(new BigDecimal("100000"))
                .build();
        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testBooking, bookingId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        testInvoice = Invoice.builder()
                .booking(testBooking)
                .invoiceNumber("INV-2025-001")
                .subtotal(new BigDecimal("100000"))
                .taxAmount(new BigDecimal("18000"))
                .totalAmount(new BigDecimal("118000"))
                .status(Invoice.InvoiceStatus.DRAFT)
                .build();
        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testInvoice, invoiceId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("generateInvoice - creates invoice successfully")
    void generateInvoice_createsSuccessfully() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

        var response = invoiceService.generateInvoice(bookingId, new BigDecimal("18"));

        assertThat(response).isNotNull();
        assertThat(response.getInvoiceNumber()).isEqualTo("INV-2025-001");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("118000"));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("generateInvoice - throws exception when booking not found")
    void generateInvoice_bookingNotFound_throwsException() {
        when(bookingRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> invoiceService.generateInvoice(bookingId, new BigDecimal("18")));
    }

    @Test
    @DisplayName("voidInvoice - voids invoice successfully")
    void voidInvoice_voidsSuccessfully() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

        var response = invoiceService.voidInvoice(invoiceId);

        assertThat(response).isNotNull();
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("voidInvoice - throws exception when invoice not found")
    void voidInvoice_invoiceNotFound_throwsException() {
        when(invoiceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> invoiceService.voidInvoice(invoiceId));
    }

    @Test
    @DisplayName("getById - returns invoice when exists")
    void getById_existingInvoice_returnsInvoice() {
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(testInvoice));

        var response = invoiceService.getById(invoiceId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(invoiceId);
    }

    @Test
    @DisplayName("getById - throws exception when invoice not found")
    void getById_nonExistingInvoice_throwsException() {
        when(invoiceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getById(invoiceId));
    }

    @Test
    @DisplayName("getByBookingId - returns invoice for booking")
    void getByBookingId_existingInvoice_returnsInvoice() {
        when(invoiceRepository.findByBookingId(bookingId)).thenReturn(Optional.of(testInvoice));

        var response = invoiceService.getByBookingId(bookingId);

        assertThat(response).isNotNull();
        assertThat(response.getInvoiceNumber()).isEqualTo("INV-2025-001");
    }

    @Test
    @DisplayName("getByBookingId - throws exception when invoice not found")
    void getByBookingId_nonExistingInvoice_throwsException() {
        when(invoiceRepository.findByBookingId(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> invoiceService.getByBookingId(bookingId));
    }

    @Test
    @DisplayName("list - returns paginated invoices")
    void list_returnsPaginatedInvoices() {
        Pageable pageable = mock(Pageable.class);
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice), pageable, 1);
        when(invoiceRepository.findAll(pageable)).thenReturn(invoicePage);

        var response = invoiceService.list(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByStatus - returns invoices by status")
    void listByStatus_returnsInvoicesByStatus() {
        Pageable pageable = mock(Pageable.class);
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice), pageable, 1);
        when(invoiceRepository.findByStatus(Invoice.InvoiceStatus.DRAFT, pageable)).thenReturn(invoicePage);

        var response = invoiceService.listByStatus(Invoice.InvoiceStatus.DRAFT, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }
}