package com.domuspacis.finance.interfaces.dto;

import com.domuspacis.finance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class FinanceDtos {
    private FinanceDtos() {}

    // ── Payment ───────────────────────────────────────────────────────────────
    public record RecordPaymentRequest(
        @NotNull UUID bookingId,
        @NotNull Payment.PaymentMethod method,
        @NotNull @Positive BigDecimal amount,
        String transactionReference
    ) {}

    public record PaymentResponse(
        UUID id, UUID bookingId, BigDecimal amount, String currency,
        String method, String status, String transactionReference,
        Instant paidAt, Instant createdAt
    ) {}

    // ── Invoice ───────────────────────────────────────────────────────────────
    public record GenerateInvoiceRequest(
        @NotNull UUID bookingId,
        BigDecimal taxRate
    ) {}

    public record InvoiceResponse(
        UUID id, UUID bookingId, String invoiceNumber,
        Instant issuedAt, LocalDate dueDate,
        BigDecimal subtotal, BigDecimal taxAmount, BigDecimal totalAmount,
        String status
    ) {}

    // ── Expense ───────────────────────────────────────────────────────────────
    public record LogExpenseRequest(
        @NotNull ExpenseCategory category,
        String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate expenseDate,
        UUID approvedBy,
        String receiptReference
    ) {}

    public record ExpenseResponse(
        UUID id, String category, String description, BigDecimal amount,
        LocalDate expenseDate, UUID approvedBy, String receiptReference,
        Instant createdAt
    ) {}

    // ── RevenueTransaction ────────────────────────────────────────────────────
    public record RevenueTransactionResponse(
        UUID id, String sourceType, UUID sourceId, BigDecimal amount,
        String currency, LocalDate transactionDate, String description
    ) {}

    // ── FinancialReport ───────────────────────────────────────────────────────
    public record GenerateReportRequest(
        @NotBlank String reportType,
        Integer year, Integer quarter, String month
    ) {}

    public record FinancialReportResponse(
        UUID id, String reportType, String period,
        BigDecimal totalRevenue, BigDecimal totalExpenses, BigDecimal netIncome,
        Instant generatedAt, UUID generatedBy
    ) {}
}
