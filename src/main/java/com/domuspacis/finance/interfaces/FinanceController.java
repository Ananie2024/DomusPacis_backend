package com.domuspacis.finance.interfaces;

import com.domuspacis.finance.application.*;
import com.domuspacis.finance.domain.*;
import com.domuspacis.finance.interfaces.dto.FinanceDtos.*;
import com.domuspacis.shared.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@Tag(name = "Finance", description = "Payments, invoices, expenses and financial reports")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','FINANCE','MANAGER')")
public class FinanceController {

    private final PaymentService          paymentService;
    private final InvoiceService          invoiceService;
    private final ExpenseService          expenseService;
    private final FinancialReportService  reportService;

    // ── Payments ──────────────────────────────────────────────────────────────

    @PostMapping("/payments")
    @Operation(summary = "Record a payment against a booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest req) {
        Payment p = paymentService.recordPayment(
                req.bookingId(), req.method(), req.amount(), req.transactionReference());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded", toPaymentResponse(p)));
    }

    @GetMapping("/payments/booking/{bookingId}")
    @Operation(summary = "Get payment by booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> paymentByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.success(toPaymentResponse(paymentService.getByBookingId(bookingId))));
    }

    @PostMapping("/payments/{paymentId}/refund")
    @Operation(summary = "Refund a payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(ApiResponse.success(toPaymentResponse(paymentService.refundPayment(paymentId))));
    }

    // ── Invoices ──────────────────────────────────────────────────────────────

    @PostMapping("/invoices")
    @Operation(summary = "Generate invoice for a booking")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generateInvoice(
            @Valid @RequestBody GenerateInvoiceRequest req) {
        Invoice inv = invoiceService.generateInvoice(req.bookingId(), req.taxRate());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice generated", toInvoiceResponse(inv)));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List invoices")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> listInvoices(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.list(pageable).map(this::toInvoiceResponse)));
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toInvoiceResponse(invoiceService.getById(id))));
    }

    @GetMapping("/invoices/booking/{bookingId}")
    @Operation(summary = "Get invoice by booking")
    public ResponseEntity<ApiResponse<InvoiceResponse>> invoiceByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.success(toInvoiceResponse(invoiceService.getByBookingId(bookingId))));
    }

    @DeleteMapping("/invoices/{id}/void")
    @Operation(summary = "Void an invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> voidInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toInvoiceResponse(invoiceService.voidInvoice(id))));
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    @PostMapping("/expenses")
    @Operation(summary = "Log an expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> logExpense(
            @Valid @RequestBody LogExpenseRequest req) {
        Expense e = expenseService.logExpense(req.category(), req.description(),
                req.amount(), req.expenseDate(), req.approvedBy(), req.receiptReference());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense logged", toExpenseResponse(e)));
    }

    @GetMapping("/expenses")
    @Operation(summary = "List expenses")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> listExpenses(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.list(pageable).map(this::toExpenseResponse)));
    }

    @GetMapping("/expenses/{id}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toExpenseResponse(expenseService.getById(id))));
    }

    @PatchMapping("/expenses/{id}/approve")
    @Operation(summary = "Approve expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> approveExpense(
            @PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        // In a real app we'd resolve the employee UUID from the principal
        return ResponseEntity.ok(ApiResponse.success(
                toExpenseResponse(expenseService.approveExpense(id, null))));
    }

    @DeleteMapping("/expenses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete expense record")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable UUID id) {
        expenseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted", null));
    }

    // ── Financial Reports ─────────────────────────────────────────────────────

    @PostMapping("/reports/monthly")
    @Operation(summary = "Generate monthly financial report")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> monthly(
            @RequestParam String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        FinancialReport r = reportService.generateMonthlyReport(ym, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated", toReportResponse(r)));
    }

    @PostMapping("/reports/quarterly")
    @Operation(summary = "Generate quarterly financial report")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> quarterly(
            @RequestParam int year, @RequestParam int quarter) {
        FinancialReport r = reportService.generateQuarterlyReport(year, quarter, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated", toReportResponse(r)));
    }

    @PostMapping("/reports/annual")
    @Operation(summary = "Generate annual financial report")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> annual(@RequestParam int year) {
        FinancialReport r = reportService.generateAnnualReport(year, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated", toReportResponse(r)));
    }

    @GetMapping("/reports")
    @Operation(summary = "List all financial reports")
    public ResponseEntity<ApiResponse<Page<FinancialReportResponse>>> listReports(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.list(pageable).map(this::toReportResponse)));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get report by ID")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toReportResponse(reportService.getById(id))));
    }

    // ── mappers ───────────────────────────────────────────────────────────────

    private PaymentResponse toPaymentResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getBooking().getId(), p.getAmount(), p.getCurrency(),
                p.getMethod().name(), p.getStatus().name(), p.getTransactionReference(),
                p.getPaidAt(), p.getCreatedAt());
    }

    private InvoiceResponse toInvoiceResponse(Invoice i) {
        return new InvoiceResponse(i.getId(), i.getBooking().getId(), i.getInvoiceNumber(),
                i.getIssuedAt(), i.getDueDate(), i.getSubtotal(), i.getTaxAmount(),
                i.getTotalAmount(), i.getStatus().name());
    }

    private ExpenseResponse toExpenseResponse(Expense e) {
        return new ExpenseResponse(e.getId(), e.getCategory().name(), e.getDescription(),
                e.getAmount(), e.getExpenseDate(), e.getApprovedBy(),
                e.getReceiptReference(), e.getCreatedAt());
    }

    private FinancialReportResponse toReportResponse(FinancialReport r) {
        return new FinancialReportResponse(r.getId(), r.getReportType().name(), r.getPeriod(),
                r.getTotalRevenue(), r.getTotalExpenses(), r.getNetIncome(),
                r.getGeneratedAt(), r.getGeneratedBy());
    }
}
