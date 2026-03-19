package com.domuspacis.customer.interfaces.dto;

import com.domuspacis.shared.domain.Address;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class CustomerDtos {
    private CustomerDtos() {}

    public record CreateCustomerRequest(
        @NotBlank String fullName,
        @Email String email,
        String phone,
        String nationality,
        String idNumber,
        Address address
    ) {}

    public record UpdateCustomerRequest(
        String fullName,
        @Email String email,
        String phone,
        String nationality,
        String idNumber,
        Address address,
        String segment,
        String notes
    ) {}

    public record CustomerResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String phone,
        String nationality,
        String idNumber,
        Address address,
        String segment,
        Instant createdAt
    ) {}

    public record CustomerSummaryResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String segment
    ) {}
}
