package com.domuspacis.staff.application;

import com.domuspacis.shared.exception.ResourceNotFoundException;
import com.domuspacis.staff.domain.Employee;
import com.domuspacis.staff.domain.EmployeeRole;
import com.domuspacis.staff.infrastructure.EmployeeRepository;
import com.domuspacis.staff.infrastructure.EmployeeRoleRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StaffService Unit Tests")
class StaffServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeRoleRepository roleRepository;
    @Mock private com.domuspacis.auth.infrastructure.UserRepository userRepository;

    @InjectMocks private StaffService staffService;

    private Employee testEmployee;
    private EmployeeRole testRole;
    private UUID employeeId;
    private UUID roleId;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        roleId = UUID.randomUUID();

        testRole = EmployeeRole.builder()
                .title("Manager")
                .description("Hotel Manager")
                .permissions(List.of("READ", "WRITE", "DELETE"))
                .build();
        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testRole, roleId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        testEmployee = Employee.builder()
                .fullName("John Manager")
                .nationalId("1199001234567")
                .phone("+250788000001")
                .department("MANAGEMENT")
                .baseSalary(new BigDecimal("500000"))
                .isActive(true)
                .build();
        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testEmployee, employeeId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("createEmployee - creates employee successfully")
    void createEmployee_createsSuccessfully() {
        when(roleRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        var response = staffService.createEmployee(
                "John Manager", "1199001234567", "+250788000001",
                null, null, null,
                LocalDate.now(), new BigDecimal("500000"), null, null
        );

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("John Manager");
        assertThat(response.getDepartment()).isEqualTo("MANAGEMENT");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("assignRole - assigns role to employee")
    void assignRole_assignsSuccessfully() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(testRole));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        var response = staffService.assignRole(employeeId, roleId);

        assertThat(response).isNotNull();
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("assignRole - throws exception when employee not found")
    void assignRole_employeeNotFound_throwsException() {
        when(employeeRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> staffService.assignRole(employeeId, roleId));
    }

    @Test
    @DisplayName("assignRole - throws exception when role not found")
    void assignRole_roleNotFound_throwsException() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
        when(roleRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> staffService.assignRole(employeeId, roleId));
    }

    @Test
    @DisplayName("updateSalary - updates employee salary")
    void updateSalary_updatesSuccessfully() {
        BigDecimal newSalary = new BigDecimal("600000");
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        var response = staffService.updateSalary(employeeId, newSalary);

        assertThat(response).isNotNull();
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("updateSalary - throws exception when employee not found")
    void updateSalary_employeeNotFound_throwsException() {
        when(employeeRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> staffService.updateSalary(employeeId, new BigDecimal("600000")));
    }

    @Test
    @DisplayName("terminateEmployee - terminates employee")
    void terminateEmployee_terminatesSuccessfully() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        staffService.terminateEmployee(employeeId);

        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("terminateEmployee - throws exception when employee not found")
    void terminateEmployee_employeeNotFound_throwsException() {
        when(employeeRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> staffService.terminateEmployee(employeeId));
    }

    @Test
    @DisplayName("getById - returns employee when exists")
    void getById_existingEmployee_returnsEmployee() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(testEmployee));

        var response = staffService.getById(employeeId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(employeeId);
    }

    @Test
    @DisplayName("getById - throws exception when employee not found")
    void getById_nonExistingEmployee_throwsException() {
        when(employeeRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> staffService.getById(employeeId));
    }

    @Test
    @DisplayName("listActive - returns paginated active employees")
    void listActive_returnsPaginatedEmployees() {
        Pageable pageable = mock(Pageable.class);
        Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee), pageable, 1);
        when(employeeRepository.findByIsActiveTrue(pageable)).thenReturn(employeePage);

        var response = staffService.listActive(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("searchByName - returns matching employees")
    void searchByName_matchingQuery_returnsEmployees() {
        Pageable pageable = mock(Pageable.class);
        Page<Employee> employeePage = new PageImpl<>(List.of(testEmployee), pageable, 1);
        when(employeeRepository.searchByName("John", pageable)).thenReturn(employeePage);

        var response = staffService.searchByName("John", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getFullName()).isEqualTo("John Manager");
    }

    @Test
    @DisplayName("listByDepartment - returns employees by department")
    void listByDepartment_returnsEmployeesByDepartment() {
        when(employeeRepository.findByDepartment("MANAGEMENT")).thenReturn(List.of(testEmployee));

        var response = staffService.listByDepartment("MANAGEMENT");

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getDepartment()).isEqualTo("MANAGEMENT");
    }

    @Test
    @DisplayName("createRole - creates role successfully")
    void createRole_createsSuccessfully() {
        when(roleRepository.save(any(EmployeeRole.class))).thenReturn(testRole);

        var response = staffService.createRole("Manager", "Hotel Manager", List.of("READ", "WRITE"));

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Manager");
        assertThat(response.getPermissions()).contains("READ", "WRITE");
        verify(roleRepository).save(any(EmployeeRole.class));
    }

    @Test
    @DisplayName("listRoles - returns all roles")
    void listRoles_returnsAllRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(testRole));

        var response = staffService.listRoles();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getTitle()).isEqualTo("Manager");
    }
}