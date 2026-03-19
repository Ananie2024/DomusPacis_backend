package com.domuspacis.staff.infrastructure;
import com.domuspacis.staff.domain.ContractType;
import com.domuspacis.staff.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByUserId(UUID userId);
    Optional<Employee> findByNationalId(String nationalId);
    List<Employee> findByDepartment(String department);
    List<Employee> findByContractType(ContractType contractType);
    Page<Employee> findByIsActiveTrue(Pageable pageable);
    @Query("SELECT e FROM Employee e WHERE LOWER(e.fullName) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Employee> searchByName(@Param("q") String q, Pageable pageable);
    long countByIsActiveTrue();
}
