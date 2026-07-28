package com.domuspacis.finance.application;

import com.domuspacis.booking.domain.Booking;
import com.domuspacis.booking.domain.Room;
import com.domuspacis.booking.domain.ServiceAsset;
import com.domuspacis.finance.domain.Invoice;
import com.domuspacis.finance.domain.Payment;
import com.domuspacis.finance.infrastructure.InvoiceRepository;
import com.domuspacis.finance.infrastructure.PaymentRepository;
import com.domuspacis.finance.infrastructure.RevenueTransactionRepository;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private InvoiceService invoiceService;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private RevenueTransactionRepository revenueTransactionRepository;
    @Mock private com.domuspacis.booking.infrastructure.BookingRepository bookingRepository;
    @Mock private com.domuspacis.booking.infrastructure.ServiceAssetRepository serviceAssetRepository;

    @InjectMocks private PaymentService paymentService;

    private Payment testPayment;
    private Booking testBooking;
    private UUID paymentId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
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

        testPayment = Payment.builder()
                .booking(testBooking)
                .amount(new BigDecimal("100000"))
                .method(Payment.PaymentMethod.CASH)
                .status(Payment.PaymentStatus.PAID)
                .build();
        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testPayment, paymentId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("recordPayment - records payment successfully")
    void recordPayment_createsSuccessfully() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(invoiceService.getByBookingId(bookingId)).thenReturn(null);

        var response = paymentService.recordPayment(
                bookingId, Payment.PaymentMethod.CASH,
                new BigDecimal("100000"), "Cash payment"
        );

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.getMethod()).isEqualTo(Payment.PaymentMethod.CASH);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("recordPayment - throws exception when booking not found")
    void recordPayment_bookingNotFound_throwsException() {
        when(bookingRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> paymentService.recordPayment(
                    bookingId, Payment.PaymentMethod.CASH,
                    new BigDecimal("100000"), "Cash payment"
            ));
    }

    @Test
    @DisplayName("refundPayment - refunds payment successfully")
    void refundPayment_refundsSuccessfully() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        var response = paymentService.refundPayment(paymentId);

        assertThat(response).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("refundPayment - throws exception when payment not found")
    void refundPayment_paymentNotFound_throwsException() {
        when(paymentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> paymentService.refundPayment(paymentId));
    }

    @Test
    @DisplayName("getByBookingId - returns payment for booking")
    void getByBookingId_existingPayment_returnsPayment() {
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(testPayment));

        var response = paymentService.getByBookingId(bookingId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(paymentId);
    }

    @Test
    @DisplayName("getByBookingId - throws exception when payment not found")
    void getByBookingId_nonExistingPayment_throwsException() {
        when(paymentRepository.findByBookingId(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> paymentService.getByBookingId(bookingId));
    }

    @Test
    @DisplayName("listByStatus - returns paginated payments by status")
    void listByStatus_returnsPaginatedPayments() {
        Pageable pageable = mock(Pageable.class);
        Page<Payment> paymentPage = new PageImpl<>(List.of(testPayment), pageable, 1);
        when(paymentRepository.findByStatus(Payment.PaymentStatus.PAID, pageable)).thenReturn(paymentPage);

        var response = paymentService.listByStatus(Payment.PaymentStatus.PAID, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }
}