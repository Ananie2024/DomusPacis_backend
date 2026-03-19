package com.domuspacis.finance.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.finance.domain.Expense;
import com.domuspacis.finance.domain.ExpenseCategory;
import com.domuspacis.finance.infrastructure.ExpenseRepository;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public Expense logExpense(ExpenseCategory category, String description,
                              BigDecimal amount, LocalDate date,
                              UUID approvedBy, String receiptRef) {
        Expense expense = Expense.builder()
                .category(category)
                .description(description)
                .amount(amount)
                .expenseDate(date)
                .approvedBy(approvedBy)
                .receiptReference(receiptRef)
                .build();
        return expenseRepository.save(expense);
    }

    @Audited("APPROVE_EXPENSE")
    public Expense approveExpense(UUID expenseId, UUID approverId) {
        Expense expense = findById(expenseId);
        expense.setApprovedBy(approverId);
        return expenseRepository.save(expense);
    }

    @Transactional(readOnly = true)
    public Expense getById(UUID id) { return findById(id); }

    @Transactional(readOnly = true)
    public Page<Expense> list(Pageable pageable) { return expenseRepository.findAll(pageable); }

    @Transactional(readOnly = true)
    public Page<Expense> listByCategory(ExpenseCategory category, Pageable pageable) {
        return expenseRepository.findByCategory(category, pageable);
    }

    @Transactional(readOnly = true)
    public List<Expense> listByDateRange(LocalDate from, LocalDate to) {
        return expenseRepository.findByExpenseDateBetween(from, to);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalByDateRange(LocalDate from, LocalDate to) {
        return expenseRepository.sumByDateRange(from, to);
    }

    public void delete(UUID id) {
        if (!expenseRepository.existsById(id)) throw new ResourceNotFoundException("Expense", id);
        expenseRepository.deleteById(id);
    }

    private Expense findById(UUID id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }
}
