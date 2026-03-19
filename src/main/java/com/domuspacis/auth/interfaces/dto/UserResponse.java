package com.domuspacis.auth.interfaces.dto;
import java.util.UUID;
public record UserResponse(UUID id, String email, String firstName, String lastName, String role, Boolean isActive) {}
