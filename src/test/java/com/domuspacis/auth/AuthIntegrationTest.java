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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth Integration Tests")
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

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
}
