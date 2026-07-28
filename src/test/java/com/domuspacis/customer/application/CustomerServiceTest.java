package com.domuspacis.customer.application;

import com.domuspacis.auth.domain.User;
import com.domuspacis.customer.domain.Customer;
import com.domuspacis.customer.infrastructure.CustomerRepository;
import com.domuspacis.customer.interfaces.dto.CustomerDtos.CreateCustomerRequest;
import com.domuspacis.customer.interfaces.dto.CustomerDtos.UpdateCustomerRequest;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private com.domuspacis.auth.infrastructure.UserRepository userRepository;

    @InjectMocks private CustomerService customerService;

    private Customer testCustomer;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        testCustomer = Customer.builder()
                .fullName("Jean Damascene")
                .email("jean@test.rw")
                .phone("+250788000001")
                .nationality("Rwandan")
                .idNumber("1234567890")
                .segment("REGULAR")
                .build();

        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testCustomer, customerId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("createCustomer - creates customer successfully")
    void createCustomer_createsSuccessfully() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Jean Damascene", "jean@test.rw", "+250788000001",
                "Rwandan", "1234567890", null
        );

        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        var response = customerService.createCustomer(request);

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Jean Damascene");
        assertThat(response.email()).isEqualTo("jean@test.rw");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("createCustomerForUser - creates customer linked to user")
    void createCustomerForUser_createsSuccessfully() {
        UUID userId = UUID.randomUUID();
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Jean Damascene", "jean@test.rw", "+250788000001",
                "Rwandan", "1234567890", null
        );

        User user = User.builder()
                .email("jean@test.rw")
                .firstName("Jean")
                .lastName("Damascene")
                .build();
        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        var response = customerService.createCustomerForUser(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Jean Damascene");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("createCustomerForUser - throws exception when user not found")
    void createCustomerForUser_userNotFound_throwsException() {
        UUID userId = UUID.randomUUID();
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Jean Damascene", "jean@test.rw", "+250788000001",
                "Rwandan", "1234567890", null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> customerService.createCustomerForUser(userId, request));
    }

    @Test
    @DisplayName("getById - returns customer when exists")
    void getById_existingCustomer_returnsCustomer() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));

        var response = customerService.getById(customerId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(customerId);
        assertThat(response.fullName()).isEqualTo("Jean Damascene");
    }

    @Test
    @DisplayName("getById - throws exception when customer not found")
    void getById_nonExistingCustomer_throwsException() {
        when(customerRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getById(customerId));
    }

    @Test
    @DisplayName("getByUserId - returns customer for user")
    void getByUserId_existingCustomer_returnsCustomer() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .email("jean@test.rw")
                .firstName("Jean")
                .lastName("Damascene")
                .build();
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testCustomer.setUser(user);

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(testCustomer));

        var response = customerService.getByUserId(userId);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("jean@test.rw");
    }

    @Test
    @DisplayName("getByUserId - throws exception when customer not found")
    void getByUserId_nonExistingCustomer_throwsException() {
        UUID userId = UUID.randomUUID();
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getByUserId(userId));
    }

    @Test
    @DisplayName("listAll - returns paginated customers")
    void listAll_returnsPaginatedCustomers() {
        Pageable pageable = mock(Pageable.class);
        Page<Customer> customerPage = new PageImpl<>(List.of(testCustomer), pageable, 1);
        when(customerRepository.findAll(pageable)).thenReturn(customerPage);

        var response = customerService.listAll(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("search - returns matching customers")
    void search_matchingQuery_returnsCustomers() {
        Pageable pageable = mock(Pageable.class);
        Page<Customer> customerPage = new PageImpl<>(List.of(testCustomer), pageable, 1);
        when(customerRepository.search("Jean", pageable)).thenReturn(customerPage);

        var response = customerService.search("Jean", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).fullName()).isEqualTo("Jean Damascene");
    }

    @Test
    @DisplayName("update - updates customer successfully")
    void update_existingCustomer_updatesSuccessfully() {
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "Jean Updated", "jean.updated@test.rw", "+250788000002",
                "Rwandan", "1234567890", null, "VIP", null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        var response = customerService.update(customerId, request);

        assertThat(response).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("update - throws exception when customer not found")
    void update_nonExistingCustomer_throwsException() {
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "Jean Updated", "jean.updated@test.rw", "+250788000002",
                "Rwandan", "1234567890", null, "VIP", null
        );

        when(customerRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.update(customerId, request));
    }

    @Test
    @DisplayName("delete - deletes customer successfully")
    void delete_existingCustomer_deletesSuccessfully() {
        when(customerRepository.existsById(customerId)).thenReturn(true);
        doNothing().when(customerRepository).deleteById(customerId);

        customerService.delete(customerId);

        verify(customerRepository).deleteById(customerId);
    }

    @Test
    @DisplayName("delete - throws exception when customer not found")
    void delete_nonExistingCustomer_throwsException() {
        when(customerRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.delete(customerId));
    }
}