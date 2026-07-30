package com.domuspacis.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "Access token is required")
        String accessToken,

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}