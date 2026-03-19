package com.domuspacis.booking;

import com.domuspacis.AbstractIntegrationTest;
import com.domuspacis.auth.application.AuthService;
import com.domuspacis.auth.interfaces.dto.RegisterRequest;
import com.domuspacis.booking.application.ServiceAssetService;
import com.domuspacis.booking.interfaces.dto.BookingDtos.*;
import com.domuspacis.customer.application.CustomerService;
import com.domuspacis.customer.interfaces.dto.CustomerDtos.CreateCustomerRequest;
import com.domuspacis.shared.util.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Booking Integration Tests")
class BookingIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired AuthService      authService;
    @Autowired ServiceAssetService assetService;
    @Autowired CustomerService  customerService;

    private String adminToken;
    private UUID   customerId;
    private UUID   assetId;

    @BeforeEach
    void setUp() {
        // Create admin user and get token
        try {
            var reg = new RegisterRequest("booking.admin@domuspacis.org", "Admin1234!", "Booking", "Admin");
            var auth = authService.register(reg);
            adminToken = auth.accessToken();
        } catch (Exception ignored) {
            var login = new com.domuspacis.auth.interfaces.dto.LoginRequest(
                    "booking.admin@domuspacis.org", "Admin1234!");
            adminToken = authService.login(login).accessToken();
        }

        // Create a customer
        var custReq = new CreateCustomerRequest("Jean Damascene", "jd@test.rw", "+250788000001", "Rwandan", "1234567890", null);
        var cust = customerService.createCustomer(custReq);
        customerId = cust.id();

        // Create a room asset
        var assetReq = new CreateServiceAssetRequest("ROOM", "Room 101", "Standard room", 2,
                new BigDecimal("50000"), "PER_NIGHT",
                "101", "SINGLE", 1,
                null, null, null, null,
                null, null, null, null, null);
        var asset = assetService.create(assetReq);
        assetId = asset.id();
    }

    @Test
    @DisplayName("Availability check returns true for free dates")
    void checkAvailability_freeDates_returnsTrue() {
        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity(
                "/api/v1/bookings/availability?assetId={a}&checkIn={ci}&checkOut={co}",
                ApiResponse.class,
                assetId,
                LocalDate.now().plusDays(10).toString(),
                LocalDate.now().plusDays(12).toString());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Authenticated user can create a booking")
    void createBooking_authenticated_returnsCreated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateBookingRequest req = new CreateBookingRequest(
                customerId, assetId,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2, "Quiet room please");

        ResponseEntity<ApiResponse> resp = restTemplate.exchange(
                "/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), ApiResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Double-booking same dates raises conflict")
    void createBooking_conflictingDates_returns409() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        LocalDate ci = LocalDate.now().plusDays(20);
        LocalDate co = LocalDate.now().plusDays(22);
        CreateBookingRequest req = new CreateBookingRequest(customerId, assetId, ci, co, 1, null);

        // First booking
        restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), ApiResponse.class);

        // Second booking same dates
        ResponseEntity<ApiResponse> resp = restTemplate.exchange(
                "/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), ApiResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Unauthenticated user cannot list bookings")
    void listBookings_noAuth_returns401or403() {
        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity("/api/v1/bookings", ApiResponse.class);
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }
}
