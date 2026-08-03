package com.domuspacis.booking.application;

import com.domuspacis.booking.domain.Booking;
import com.domuspacis.booking.domain.BookingStatus;
import com.domuspacis.booking.domain.Room;
import com.domuspacis.booking.domain.ServiceAsset;
import com.domuspacis.booking.infrastructure.BookingRepository;
import com.domuspacis.booking.infrastructure.ServiceAssetRepository;
import com.domuspacis.booking.interfaces.dto.BookingDtos.CreateBookingRequest;
import com.domuspacis.customer.domain.Customer;
import com.domuspacis.customer.infrastructure.CustomerRepository;
import com.domuspacis.finance.domain.Payment;
import com.domuspacis.finance.domain.RevenueSourceType;
import com.domuspacis.finance.domain.RevenueTransaction;
import com.domuspacis.finance.infrastructure.InvoiceRepository;
import com.domuspacis.finance.infrastructure.PaymentRepository;
import com.domuspacis.finance.infrastructure.RevenueTransactionRepository;
import com.domuspacis.inventory.infrastructure.FoodOrderRepository;
import com.domuspacis.shared.exception.BookingConflictException;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ServiceAssetRepository serviceAssetRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AvailabilityService availabilityService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FoodOrderRepository foodOrderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private RevenueTransactionRepository revenueTransactionRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks private BookingService bookingService;

    private ServiceAsset testAsset;
    private Customer testCustomer;
    private Booking testBooking;
    private UUID assetId;
    private UUID customerId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        testAsset = Room.builder()
                .id(assetId)
                .name("Room 101")
                .assetType(com.domuspacis.booking.domain.ServiceAsset.AssetType.ROOM)
                .pricePerUnit(new BigDecimal("50000"))
                .pricingUnit(com.domuspacis.booking.domain.PricingUnit.PER_NIGHT)
                .capacity(2)
                .isAvailable(true)
                .roomNumber("101")
                .roomType(com.domuspacis.booking.domain.Room.RoomType.SINGLE)
                .floor(1)
                .build();

        testCustomer = Customer.builder()
                .fullName("Jean Damascene")
                .email("jean@test.rw")
                .phone("+250788000001")
                .build();
        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testCustomer, customerId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        testBooking = Booking.builder()
                .customer(testCustomer)
                .serviceAsset(testAsset)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(7))
                .numberOfGuests(2)
                .status(BookingStatus.PENDING)
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

        // Setup security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("jean@test.rw");
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("createBooking - creates booking for new customer")
    void createBooking_newCustomer_createsSuccessfully() {
        CreateBookingRequest request = new CreateBookingRequest(
                assetId,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2,
                "Quiet room",
                "Jean",
                "Damascene",
                "jean@test.rw",
                "+250788000001"
        );

        when(serviceAssetRepository.findById(assetId)).thenReturn(Optional.of(testAsset));
        when(serviceAssetRepository.findByIdWithLock(assetId)).thenReturn(Optional.of(testAsset));
        when(customerRepository.findByEmail("jean@test.rw")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        when(availabilityService.isAvailable(assetId, request.checkInDate(), request.checkOutDate())).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        var response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("PENDING");
        verify(customerRepository).save(any(Customer.class));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - uses existing customer")
    void createBooking_existingCustomer_usesExisting() {
        CreateBookingRequest request = new CreateBookingRequest(
                assetId,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2,
                null,
                "Jean",
                "Damascene",
                "jean@test.rw",
                "+250788000001"
        );

        when(serviceAssetRepository.findById(assetId)).thenReturn(Optional.of(testAsset));
        when(serviceAssetRepository.findByIdWithLock(assetId)).thenReturn(Optional.of(testAsset));
        when(customerRepository.findByEmail("jean@test.rw")).thenReturn(Optional.of(testCustomer));
        when(availabilityService.isAvailable(assetId, request.checkInDate(), request.checkOutDate())).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        var response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        verify(customerRepository, never()).save(any(Customer.class));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - throws exception when asset not found")
    void createBooking_assetNotFound_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
                UUID.randomUUID(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2,
                null,
                "Jean",
                "Damascene",
                "jean@test.rw",
                "+250788000001"
        );

        when(serviceAssetRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.createBooking(request));
    }

    @Test
    @DisplayName("createBooking - throws exception when check-out is not after check-in")
    void createBooking_invalidDates_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
                assetId,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(5), // check-out before check-in
                2,
                null,
                "Jean",
                "Damascene",
                "jean@test.rw",
                "+250788000001"
        );

        when(serviceAssetRepository.findById(assetId)).thenReturn(Optional.of(testAsset));
        when(serviceAssetRepository.findByIdWithLock(assetId)).thenReturn(Optional.of(testAsset));

        assertThrows(BusinessRuleViolationException.class, () -> bookingService.createBooking(request));
    }

    @Test
    @DisplayName("createBooking - throws exception when asset not available")
    void createBooking_assetNotAvailable_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest(
                assetId,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2,
                null,
                "Jean",
                "Damascene",
                "jean@test.rw",
                "+250788000001"
        );

        when(serviceAssetRepository.findById(assetId)).thenReturn(Optional.of(testAsset));
        when(serviceAssetRepository.findByIdWithLock(assetId)).thenReturn(Optional.of(testAsset));
        when(availabilityService.isAvailable(assetId, request.checkInDate(), request.checkOutDate())).thenReturn(false);

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(request));
    }

    @Test
    @DisplayName("confirmBooking - confirms PENDING booking")
    void confirmBooking_pendingBooking_confirmsSuccessfully() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        var response = bookingService.confirmBooking(bookingId);

        assertThat(response).isNotNull();
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("confirmBooking - throws exception for non-PENDING booking")
    void confirmBooking_nonPendingBooking_throwsException() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        assertThrows(BusinessRuleViolationException.class, () -> bookingService.confirmBooking(bookingId));
    }

    @Test
    @DisplayName("checkIn - checks in CONFIRMED booking")
    void checkIn_confirmedBooking_checksInSuccessfully() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        var response = bookingService.checkIn(bookingId);

        assertThat(response).isNotNull();
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("checkIn - throws exception for non-CONFIRMED booking")
    void checkIn_nonConfirmedBooking_throwsException() {
        testBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        assertThrows(BusinessRuleViolationException.class, () -> bookingService.checkIn(bookingId));
    }

    @Test
    @DisplayName("completeBooking - completes CHECKED_IN booking")
    void completeBooking_checkedInBooking_completesSuccessfully() {
        testBooking.setStatus(BookingStatus.CHECKED_IN);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        var response = bookingService.completeBooking(bookingId);

        assertThat(response).isNotNull();
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("completeBooking - throws exception for non-CHECKED_IN booking")
    void completeBooking_nonCheckedInBooking_throwsException() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        assertThrows(BusinessRuleViolationException.class, () -> bookingService.completeBooking(bookingId));
    }

    @Test
    @DisplayName("cancelBooking - cancels non-COMPLETED booking")
    void cancelBooking_nonCompletedBooking_cancelsSuccessfully() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        var response = bookingService.cancelBooking(bookingId, "Customer request");

        assertThat(response).isNotNull();
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(testBooking.getSpecialRequests()).contains("CANCELLATION REASON: Customer request");
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("cancelBooking - throws exception for COMPLETED booking")
    void cancelBooking_completedBooking_throwsException() {
        testBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        assertThrows(BusinessRuleViolationException.class, () -> bookingService.cancelBooking(bookingId, "reason"));
    }

    @Test
    @DisplayName("deleteBooking - deletes booking with no dependencies")
    void deleteBooking_noDependencies_deletesSuccessfully() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(foodOrderRepository.countByBookingId(bookingId)).thenReturn(0L);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(invoiceRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(revenueTransactionRepository.findBySourceTypeAndSourceId(
                RevenueSourceType.BOOKING, bookingId)).thenReturn(Optional.empty());

        bookingService.deleteBooking(bookingId);

        verify(bookingRepository).delete(testBooking);
    }

    @Test
    @DisplayName("deleteBooking - throws exception when booking has food orders")
    void deleteBooking_withFoodOrders_throwsException() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(foodOrderRepository.countByBookingId(bookingId)).thenReturn(2L);

        assertThrows(BusinessRuleViolationException.class,
                () -> bookingService.deleteBooking(bookingId));

        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    @DisplayName("deleteBooking - throws exception when booking has a payment")
    void deleteBooking_withPayment_throwsException() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(foodOrderRepository.countByBookingId(bookingId)).thenReturn(0L);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(mock(Payment.class)));

        assertThrows(BusinessRuleViolationException.class,
                () -> bookingService.deleteBooking(bookingId));

        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    @DisplayName("deleteBooking - throws exception when booking has an invoice")
    void deleteBooking_withInvoice_throwsException() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(foodOrderRepository.countByBookingId(bookingId)).thenReturn(0L);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(invoiceRepository.findByBookingId(bookingId)).thenReturn(Optional.of(mock(
                com.domuspacis.finance.domain.Invoice.class)));

        assertThrows(BusinessRuleViolationException.class,
                () -> bookingService.deleteBooking(bookingId));

        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    @DisplayName("deleteBooking - deletes revenue transaction before booking")
    void deleteBooking_withRevenueTransaction_deletesTransactionFirst() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(foodOrderRepository.countByBookingId(bookingId)).thenReturn(0L);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(invoiceRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        RevenueTransaction rt = RevenueTransaction.builder()
                .sourceType(RevenueSourceType.BOOKING)
                .sourceId(bookingId)
                .amount(new BigDecimal("100000"))
                .build();
        when(revenueTransactionRepository.findBySourceTypeAndSourceId(
                RevenueSourceType.BOOKING, bookingId)).thenReturn(Optional.of(rt));

        bookingService.deleteBooking(bookingId);

        verify(revenueTransactionRepository).delete(rt);
        verify(bookingRepository).delete(testBooking);
    }

    @Test
    @DisplayName("deleteBooking - throws exception when booking not found")
    void deleteBooking_notFound_throwsException() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.deleteBooking(bookingId));

        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    @DisplayName("overrideDates - updates dates when available")
    void overrideDates_availableDates_updatesSuccessfully() {
        LocalDate newCheckIn = LocalDate.now().plusDays(10);
        LocalDate newCheckOut = LocalDate.now().plusDays(12);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(availabilityService.isAvailableExcluding(assetId, newCheckIn, newCheckOut, bookingId)).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        var response = bookingService.overrideDates(bookingId, newCheckIn, newCheckOut);

        assertThat(response).isNotNull();
        assertThat(testBooking.getCheckInDate()).isEqualTo(newCheckIn);
        assertThat(testBooking.getCheckOutDate()).isEqualTo(newCheckOut);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("overrideDates - throws exception when dates not available")
    void overrideDates_unavailableDates_throwsException() {
        LocalDate newCheckIn = LocalDate.now().plusDays(10);
        LocalDate newCheckOut = LocalDate.now().plusDays(12);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(availabilityService.isAvailableExcluding(assetId, newCheckIn, newCheckOut, bookingId)).thenReturn(false);

        assertThrows(BookingConflictException.class, 
            () -> bookingService.overrideDates(bookingId, newCheckIn, newCheckOut));
    }

    @Test
    @DisplayName("getById - returns booking for owner")
    void getById_owner_returnsBooking() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        var response = bookingService.getById(bookingId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("getById - returns booking for staff")
    void getById_staff_returnsBooking() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        org.springframework.security.core.GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = java.util.List.of(authority);
        doReturn(authorities).when(authentication).getAuthorities();

        var response = bookingService.getById(bookingId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("getById - throws exception for unauthorized user")
    void getById_unauthorizedUser_throwsException() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(authentication.getName()).thenReturn("other@test.rw");

        assertThrows(AccessDeniedException.class, () -> bookingService.getById(bookingId));
    }

    @Test
    @DisplayName("listAll - returns paginated bookings")
    void listAll_returnsPaginatedBookings() {
        Pageable pageable = mock(Pageable.class);
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking), pageable, 1);
        when(bookingRepository.findAll(pageable)).thenReturn(bookingPage);

        var response = bookingService.listAll(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByCustomer - returns customer bookings")
    void listByCustomer_returnsCustomerBookings() {
        Pageable pageable = mock(Pageable.class);
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking), pageable, 1);
        when(bookingRepository.findByCustomerId(customerId, pageable)).thenReturn(bookingPage);

        var response = bookingService.listByCustomer(customerId, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("listByStatus - returns bookings by status")
    void listByStatus_returnsBookingsByStatus() {
        Pageable pageable = mock(Pageable.class);
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking), pageable, 1);
        when(bookingRepository.findByStatus(BookingStatus.PENDING, pageable)).thenReturn(bookingPage);

        var response = bookingService.listByStatus(BookingStatus.PENDING, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("checkAvailability - delegates to availability service")
    void checkAvailability_delegatesToAvailabilityService() {
        when(availabilityService.isAvailable(assetId, 
                LocalDate.now().plusDays(5), 
                LocalDate.now().plusDays(7))).thenReturn(true);

        boolean available = bookingService.checkAvailability(
                assetId,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7)
        );

        assertThat(available).isTrue();
        verify(availabilityService).isAvailable(assetId, 
                LocalDate.now().plusDays(5), 
                LocalDate.now().plusDays(7));
    }

    @Test
    @DisplayName("createBooking - calculates correct total")
    void createBooking_calculatesCorrectTotal() {
        LocalDate checkIn = LocalDate.of(2025, 1, 1);
        LocalDate checkOut = LocalDate.of(2025, 1, 4); // 3 nights

        CreateBookingRequest request = new CreateBookingRequest(
                assetId,
                checkIn,
                checkOut,
                2,
                null,
                "Jean",
                "Damascene",
                "jean@test.rw",
                "+250788000001"
        );

        when(serviceAssetRepository.findById(assetId)).thenReturn(Optional.of(testAsset));
        when(serviceAssetRepository.findByIdWithLock(assetId)).thenReturn(Optional.of(testAsset));
        when(customerRepository.findByEmail("jean@test.rw")).thenReturn(Optional.of(testCustomer));
        when(availabilityService.isAvailable(assetId, checkIn, checkOut)).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(bookingId);
            return b;
        });

        var response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("150000")); // 3 * 50000
    }
}
