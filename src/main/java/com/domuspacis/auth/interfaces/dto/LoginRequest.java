package com.domuspacis.auth.interfaces.dto;
import jakarta.validation.constraints.*;
public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=********]";
    }
}
