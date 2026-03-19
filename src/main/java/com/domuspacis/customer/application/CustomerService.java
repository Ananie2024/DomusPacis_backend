package com.domuspacis.customer.application;

import com.domuspacis.auth.domain.User;
import com.domuspacis.auth.infrastructure.UserRepository;
import com.domuspacis.customer.domain.Customer;
import com.domuspacis.customer.infrastructure.CustomerRepository;
import com.domuspacis.customer.interfaces.dto.CustomerDtos.*;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerResponse createCustomer(CreateCustomerRequest req) {
        if (req.email() != null && customerRepository.existsByEmail(req.email())) {
            throw new BusinessRuleViolationException("Customer email already exists: " + req.email());
        }
        Customer customer = Customer.builder()
                .fullName(req.fullName())
                .email(req.email())
                .phone(req.phone())
                .nationality(req.nationality())
                .idNumber(req.idNumber())
                .address(req.address())
                .build();
        return toResponse(customerRepository.save(customer));
    }

    public CustomerResponse createCustomerForUser(UUID userId, CreateCustomerRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Customer customer = Customer.builder()
                .user(user)
                .fullName(req.fullName())
                .email(req.email() != null ? req.email() : user.getEmail())
                .phone(req.phone())
                .nationality(req.nationality())
                .idNumber(req.idNumber())
                .address(req.address())
                .build();
        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getByUserId(UUID userId) {
        return toResponse(customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer for user", userId)));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> search(String query, Pageable pageable) {
        return customerRepository.search(query, pageable).map(this::toSummary);
    }

    public CustomerResponse update(UUID id, UpdateCustomerRequest req) {
        Customer customer = findById(id);
        if (req.fullName()    != null) customer.setFullName(req.fullName());
        if (req.email()       != null) customer.setEmail(req.email());
        if (req.phone()       != null) customer.setPhone(req.phone());
        if (req.nationality() != null) customer.setNationality(req.nationality());
        if (req.idNumber()    != null) customer.setIdNumber(req.idNumber());
        if (req.address()     != null) customer.setAddress(req.address());
        if (req.segment()     != null) customer.setSegment(req.segment());
        if (req.notes()       != null) customer.setNotes(req.notes());
        return toResponse(customerRepository.save(customer));
    }

    public void delete(UUID id) {
        if (!customerRepository.existsById(id)) throw new ResourceNotFoundException("Customer", id);
        customerRepository.deleteById(id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Customer findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    public CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getUser() != null ? c.getUser().getId() : null,
                c.getFullName(), c.getEmail(), c.getPhone(),
                c.getNationality(), c.getIdNumber(), c.getAddress(),
                c.getSegment(), c.getCreatedAt());
    }

    private CustomerSummaryResponse toSummary(Customer c) {
        return new CustomerSummaryResponse(c.getId(), c.getFullName(),
                c.getEmail(), c.getPhone(), c.getSegment());
    }
}
