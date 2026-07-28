package com.domuspacis.finance.application;

import com.domuspacis.finance.domain.Expense;
import com.domuspacis.finance.domain.ExpenseCategory;
import com.domuspacis.finance.infrastructure.ExpenseRepository;
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
@DisplayName("ExpenseService Unit Tests")
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;

    @InjectMocks private ExpenseService expenseService;

    private Expense testExpense;
    private UUID expenseId;

    @BeforeEach
    void setUp() {
        expenseId = UUID.randomUUID();

        testExpense = Expense.builder()
                .category(ExpenseCategory.UTILITIES)
                .description("Monthly electricity bill")
                .amount(new BigDecimal("150000"))
                .expenseDate(LocalDate.now())
                .receiptReference("ELEC-2025-01")
                .build();

        // Set ID via reflection since it's in BaseEntity
        try {
            java.lang.reflect.Field idField = com.domuspacis.shared.domain.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testExpense, expenseId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("logExpense - creates expense successfully")
    void logExpense_createsSuccessfully() {
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        var response = expenseService.logExpense(
                ExpenseCategory.UTILITIES,
                "Monthly electricity bill",
                new BigDecimal("150000"),
                LocalDate.now(),
                null,
                "ELEC-2025-01"
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(expenseId);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("150000"));
        assertThat(response.getCategory()).isEqualTo(ExpenseCategory.UTILITIES);
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("approveExpense - approves expense successfully")
    void approveExpense_approvesSuccessfully() {
        UUID approverId = UUID.randomUUID();
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        var response = expenseService.approveExpense(expenseId, approverId);

        assertThat(response).isNotNull();
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("approveExpense - throws exception when expense not found")
    void approveExpense_nonExistingExpense_throwsException() {
        UUID approverId = UUID.randomUUID();
        when(expenseRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> expenseService.approveExpense(expenseId, approverId));
    }

    @Test
    @DisplayName("getById - returns expense when exists")
    void getById_existingExpense_returnsExpense() {
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));

        var response = expenseService.getById(expenseId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(expenseId);
    }

    @Test
    @DisplayName("getById - throws exception when expense not found")
    void getById_nonExistingExpense_throwsException() {
        when(expenseRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.getById(expenseId));
    }

    @Test
    @DisplayName("list - returns paginated expenses")
    void list_returnsPaginatedExpenses() {
        Pageable pageable = mock(Pageable.class);
        Page<Expense> expensePage = new PageImpl<>(List.of(testExpense), pageable, 1);
        when(expenseRepository.findAll(pageable)).thenReturn(expensePage);

        var response = expenseService.list(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByCategory - returns expenses by category")
    void listByCategory_returnsExpensesByCategory() {
        Pageable pageable = mock(Pageable.class);
        Page<Expense> expensePage = new PageImpl<>(List.of(testExpense), pageable, 1);
        when(expenseRepository.findByCategory(ExpenseCategory.UTILITIES, pageable)).thenReturn(expensePage);

        var response = expenseService.listByCategory(ExpenseCategory.UTILITIES, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCategory()).isEqualTo(ExpenseCategory.UTILITIES);
    }

    @Test
    @DisplayName("listByDateRange - returns expenses in date range")
    void listByDateRange_returnsExpensesInRange() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now().plusDays(7);
        when(expenseRepository.findByExpenseDateBetween(from, to)).thenReturn(List.of(testExpense));

        var response = expenseService.listByDateRange(from, to);

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
    }

    @Test
    @DisplayName("totalByDateRange - calculates correct total")
    void totalByDateRange_calculatesCorrectTotal() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now().plusDays(7);
        when(expenseRepository.sumByDateRange(from, to)).thenReturn(new BigDecimal("150000"));

        BigDecimal total = expenseService.totalByDateRange(from, to);

        assertThat(total).isEqualByComparingTo(new BigDecimal("150000"));
    }

    @Test
    @DisplayName("delete - deletes expense successfully")
    void delete_existingExpense_deletesSuccessfully() {
        when(expenseRepository.existsById(expenseId)).thenReturn(true);
        doNothing().when(expenseRepository).deleteById(expenseId);

        expenseService.delete(expenseId);

        verify(expenseRepository).deleteById(expenseId);
    }

    @Test
    @DisplayName("delete - throws exception when expense not found")
    void delete_nonExistingExpense_throwsException() {
        when(expenseRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.delete(expenseId));
    }
}