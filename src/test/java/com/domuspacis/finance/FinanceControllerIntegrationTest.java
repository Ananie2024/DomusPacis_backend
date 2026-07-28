package com.domuspacis.finance;

import com.domuspacis.AbstractIntegrationTest;
import com.domuspacis.auth.application.AuthService;
import com.domuspacis.auth.interfaces.dto.LoginRequest;
import com.domuspacis.auth.interfaces.dto.RegisterRequest;
import com.domuspacis.booking.application.BookingService;
import com.domuspacis.booking.interfaces.dto.BookingDtos.CreateBookingRequest;
import com.domuspacis.customer.application.CustomerService;
import com.domuspacis.customer.interfaces.dto.CustomerDtos.CreateCustomerRequest;
import com.domuspacis.finance.application.ExpenseService;
import com.domuspacis.finance.domain.ExpenseCategory;
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

@DisplayName("Finance Controller Integration Tests")
class FinanceControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired AuthService authService;
    @Autowired CustomerService customerService;
    @Autowired BookingService bookingService;
    @Autowired ExpenseService expenseService;

    private String adminToken;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        // Create admin user and get token
        try {
            var reg = new RegisterRequest("finance.admin@domuspacis.org", "Admin1234!", "Finance", "Admin");
            var auth = authService.register(reg);
            adminToken = auth.accessToken();
        } catch (Exception ignored) {
            var login = new LoginRequest("finance.admin@domuspacis.org", "Admin1234!");
            adminToken = authService.login(login).accessToken();
        }

        // Create a customer
        var custReq = new CreateCustomerRequest("Test Customer", "test@test.rw", "+250788000001", "Rwandan", "1234567890", null);
        var cust = customerService.createCustomer(custReq);

        // Create a booking for payment/invoice tests
        var bookingReq = new CreateBookingRequest(
                UUID.randomUUID(), // assetId - will be mocked
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                2,
                null,
                "Test",
                "Customer",
                "test@test.rw",
                "+250788000001"
        );
        // Note: bookingId will be null since we don't have a real asset
        // In real tests, we'd create an asset first
    }

    @Test
    @DisplayName("POST /api/v1/finance/expenses - logs expense")
    void logExpense_returnsCreated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var request = new org.springframework.http.HttpEntity<>(
                java.util.Map.of(
                        "category", "UTILITIES",
                        "description", "Monthly electricity bill",
                        "amount", new BigDecimal("150000"),
                        "expenseDate", LocalDate.now().toString(),
                        "receiptReference", "ELEC-2025-01"
                ),
                headers
        );

        ResponseEntity<ApiResponse> resp = restTemplate.exchange(
                "/api/v1/finance/expenses",
                HttpMethod.POST,
                request,
                ApiResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /api/v1/finance/expenses - lists expenses")
    void listExpenses_returnsPaginated() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<ApiResponse> resp = restTemplate.exchange(
                "/api/v1/finance/expenses?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /api/v1/finance/reports/monthly - generates monthly report")
    void generateMonthlyReport_returnsReport() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity(
                "/api/v1/finance/reports/monthly?year=2025&month=1",
                ApiResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /api/v1/finance/reports/quarterly - generates quarterly report")
    void generateQuarterlyReport_returnsReport() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity(
                "/api/v1/finance/reports/quarterly?year=2025&quarter=1",
                ApiResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /api/v1/finance/reports/annual - generates annual report")
    void generateAnnualReport_returnsReport() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity(
                "/api/v1/finance/reports/annual?year=2025",
                ApiResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticatedRequest_returns401() {
        ResponseEntity<ApiResponse> resp = restTemplate.getForEntity(
                "/api/v1/finance/expenses", ApiResponse.class);

        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }
}