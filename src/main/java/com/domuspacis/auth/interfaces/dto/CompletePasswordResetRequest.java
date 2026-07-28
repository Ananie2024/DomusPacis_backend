package com.domuspacis.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CompletePasswordResetRequest(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {
}