package com.domuspacis.auth;

import com.domuspacis.AbstractIntegrationTest;
import com.domuspacis.auth.interfaces.dto.AuthResponse;
import com.domuspacis.auth.interfaces.dto.LoginRequest;
import com.domuspacis.auth.interfaces.dto.RegisterRequest;
import com.domuspacis.shared.util.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth Integration Tests")
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @org.springframework.boot.test.web.server.LocalServerPort
    private int serverPort;

    @Test
    @DisplayName("Customer can register and receive JWT tokens")
    void register_returnsTokens() {
        RegisterRequest request = new RegisterRequest(
                "test.user@domuspacis.org", "SecurePass123!", "Test", "User");

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Registered user can login and receive valid JWT")
    void login_withValidCredentials_returnsJwt() {
        // First register
        RegisterRequest reg = new RegisterRequest(
                "login.test@domuspacis.org", "SecurePass123!", "Login", "Test");
        restTemplate.postForEntity("/api/v1/auth/register", reg, ApiResponse.class);

        // Then login
        LoginRequest login = new LoginRequest("login.test@domuspacis.org", "SecurePass123!");
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", login, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Login with wrong password returns 401")
    void login_withWrongPassword_returns401() {
        RegisterRequest reg = new RegisterRequest(
                "badpass@domuspacis.org", "CorrectPass123!", "Bad", "Pass");
        restTemplate.postForEntity("/api/v1/auth/register", reg, ApiResponse.class);

        LoginRequest login = new LoginRequest("badpass@domuspacis.org", "WrongPassword!");
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", login, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Protected endpoint rejects unauthenticated request")
    void protectedEndpoint_withoutToken_returns403or401() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
                "/api/v1/customers", ApiResponse.class);

        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @DisplayName("Duplicate email registration returns error")
    void register_duplicateEmail_returns422() {
        RegisterRequest req = new RegisterRequest(
                "dup@domuspacis.org", "SecurePass123!", "Dup", "User");
        restTemplate.postForEntity("/api/v1/auth/register", req, ApiResponse.class);

        // Register again with same email
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", req, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("Password reset rate limit returns 429 after exceeding limit")
    void passwordReset_rateLimitExceeded_returns429() {
        // First register a user
        RegisterRequest reg = new RegisterRequest(
                "ratelimit@domuspacis.org", "SecurePass123!", "Rate", "Limit");
        restTemplate.postForEntity("/api/v1/auth/register", reg, ApiResponse.class);

        // Send 3 password reset requests (the configured limit)
        for (int i = 0; i < 3; i++) {
            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/password-reset/initiate?email=ratelimit@domuspacis.org",
                    null, ApiResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // 4th request should be rate limited
        ResponseEntity<ApiResponse> rateLimitedResponse = restTemplate.postForEntity(
                "/api/v1/auth/password-reset/initiate?email=ratelimit@domuspacis.org",
                null, ApiResponse.class);

        assertThat(rateLimitedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(rateLimitedResponse.getBody()).isNotNull();
        assertThat(rateLimitedResponse.getBody().isSuccess()).isFalse();
        assertThat(rateLimitedResponse.getBody().getMessage())
                .contains("Too many password reset requests");
    }

    @Test
    @DisplayName("Rate limit cannot be bypassed via X-Forwarded-For header spoofing")
    void rateLimit_cannotBeBypassed_withSpoofedHeaders() {
        // Register a user
        RegisterRequest reg = new RegisterRequest(
                "spoof@domuspacis.org", "SecurePass123!", "Spoof", "Test");
        restTemplate.postForEntity("/api/v1/auth/register", reg, ApiResponse.class);

        // Create a RestTemplate that adds a different X-Forwarded-For header on each request
        RestTemplate spoofingRestTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            // Spoof a different IP address on each request
            String fakeIp = "192.168.1." + (interceptors.size() + 1);
            request.getHeaders().add("X-Forwarded-For", fakeIp);
            return execution.execute(request, body);
        });
        spoofingRestTemplate.setInterceptors(interceptors);

        // Send 3 requests with different spoofed IPs
        // If the application trusts X-Forwarded-For, each would get a fresh rate limit bucket
        // With the fix, all requests should be tracked by the actual client IP
        for (int i = 0; i < 3; i++) {
            ResponseEntity<ApiResponse> response = spoofingRestTemplate.postForEntity(
                    "http://localhost:" + serverPort + "/api/v1/auth/password-reset/initiate?email=spoof@domuspacis.org",
                    null, ApiResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // 4th request (even with a new spoofed IP) should still be rate limited
        // because the server uses request.getRemoteAddr(), not the spoofed header
        ResponseEntity<ApiResponse> rateLimitedResponse = spoofingRestTemplate.postForEntity(
                "http://localhost:" + serverPort + "/api/v1/auth/password-reset/initiate?email=spoof@domuspacis.org",
                null, ApiResponse.class);

        assertThat(rateLimitedResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(rateLimitedResponse.getBody()).isNotNull();
        assertThat(rateLimitedResponse.getBody().isSuccess()).isFalse();
        assertThat(rateLimitedResponse.getBody().getMessage())
                .contains("Too many password reset requests");
    }
}
