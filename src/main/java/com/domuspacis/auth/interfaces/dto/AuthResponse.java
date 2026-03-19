package com.domuspacis.auth.interfaces.dto;
public record AuthResponse(String accessToken, String refreshToken, String email, String role, long expiresIn) {}
