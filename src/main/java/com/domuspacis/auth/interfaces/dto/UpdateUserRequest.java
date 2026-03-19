// UpdateUserRequest.java
package com.domuspacis.auth.interfaces.dto;
import com.domuspacis.auth.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email @NotBlank String email,
        UserRole role
) {}