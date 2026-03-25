package com.domuspacis.auth.interfaces.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String userId,
        String email,
        String firstName,
        String lastName,
        String role,
        long expiresIn
) {}