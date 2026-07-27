package com.domuspacis.auth.interfaces.dto;

import com.domuspacis.auth.domain.UserRole;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String firstName,
    @NotBlank String lastName
) {
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=********"
                + ", firstName=" + firstName + ", lastName=" + lastName + "]";
    }
}
