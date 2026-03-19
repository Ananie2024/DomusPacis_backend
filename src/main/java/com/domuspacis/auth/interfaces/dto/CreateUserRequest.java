package com.domuspacis.auth.interfaces.dto;
import com.domuspacis.auth.domain.UserRole;
import jakarta.validation.constraints.*;
public record CreateUserRequest(@Email @NotBlank String email, @NotBlank @Size(min=8) String password,
    @NotBlank String firstName, @NotBlank String lastName, @NotNull UserRole role) {}
