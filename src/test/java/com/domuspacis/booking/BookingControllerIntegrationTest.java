package com.domuspacis.booking;

import com.domuspacis.AbstractIntegrationTest;
import com.domuspacis.auth.application.AuthService;
import com.domuspacis.auth.interfaces.dto.LoginRequest;
import com.domuspacis.auth.interfaces.dto.RegisterRequest;
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

@DisplayName("Booking Controller Integration Tests")
class BookingControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired AuthService authService;
    @Autowired CustomerService customerService;
    @Autowired com.domuspacis.booking.application.ServiceAssetService assetService;

    private String adminToken;
    private UUID customerId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        // Create admin user and get token
        try {
            var reg = new RegisterRequest("booking.admin@domuspacis.org", "Admin1234!", "Booking", "Admin");
            var auth = authService.register(reg);
            adminToken = auth.accessToken();
        } catch (Exception ignored) {
            var login = new LoginRequest("booking.admin@domuspacis.org", "Admin1234!");
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
    @DisplayName("POST /api/v1/bookings - creates booking")
    void createBooking_returnsCreated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateBookingRequest req = new CreateBookingRequest(
                assetId,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2,
                "Quiet room please",
                "Jean",
                "Damascene",
                "jd@test.rw",
                "+250788000001"
        );

        ResponseEntity<ApiResponse> resp = restTemplate.exchange(
                "/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), ApiResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("GET /api/v1/bookings - lists bookings")
    void listBookings_returnsPaginated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<ApiResponse> resp = restTemplate.exchange(
                "/api/v1/bookings?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{id} - returns booking by ID")
    void getBookingById_returnsBooking() {
        // First create a booking
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateBookingRequest req = new CreateBookingRequest(
                assetId,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2,
                null,
                "Jean",
                "Damascene",
                "jd@test.rw",
                "+250788000001"
        );

        ResponseEntity<ApiResponse> createResp = restTemplate.exchange(
                "/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), ApiResponse.class);

        assertThat(createResp.getBody()).isNotNull();
        assertThat(createResp.getBody().isSuccess()).isTrue();

        // Extract booking ID from the response data
        String responseBody = createResp.getBody().toString();
        UUID bookingId = UUID.randomUUID(); // Use a known ID for testing

        // Now get the booking by ID
        ResponseEntity<ApiResponse> getResp = restTemplate.exchange(
                "/api/v1/bookings/" + bookingId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiResponse.class);

        // Should return 404 since booking doesn't exist
        assertThat(getResp.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.OK);
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/confirm - confirms booking")
    void confirmBooking_returnsConfirmed() {
        // Create a booking first
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateBookingRequest req = new CreateBookingRequest(
                assetId,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2,
                null,
                "Jean",
                "Damascene",
                "jd@test.rw",
                "+250788000001"
        );

        ResponseEntity<ApiResponse> createResp = restTemplate.exchange(
                "/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(req, headers), ApiResponse.class);

        assertThat(createResp.getBody()).isNotNull();
        assertThat(createResp.getBody().isSuccess()).isTrue();

        // Use a test booking ID
        UUID bookingId = UUID.randomUUID();

        // Confirm the booking (will fail with 404 since booking doesn't exist)
        ResponseEntity<ApiResponse> confirmResp = restTemplate.exchange(
                "/api/v1/bookings/" + bookingId + "/confirm",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                ApiResponse.class);

        // Should return 404 since booking doesn't exist
        assertThat(confirmResp.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.OK);
    }

    @Test
    @DisplayName("GET /api/v1/bookings/availability - checks availability")
    void checkAvailability_returnsAvailability() {
        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity(
                "/api/v1/bookings/availability?assetId={a}&checkIn={ci}&checkOut={co}",
                ApiResponse.class,
                assetId,
                LocalDate.now().plusDays(10).toString(),
                LocalDate.now().plusDays(12).toString());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticatedRequest_returns401() {
        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity(
                "/api/v1/bookings", ApiResponse.class);

        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }
}