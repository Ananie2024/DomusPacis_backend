package com.domuspacis.tax.interfaces;

import com.domuspacis.shared.util.ApiResponse;
import com.domuspacis.tax.application.TaxCalculationService;
import com.domuspacis.tax.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tax")
@RequiredArgsConstructor
@Tag(name = "Tax & Compliance", description = "Tax records, rules, and RRA compliance reporting")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','FINANCE','MANAGER')")
public class TaxController {

    private final TaxCalculationService taxService;

    // ── Tax Records ───────────────────────────────────────────────────────────

    @PostMapping("/records/compute-vat")
    @Operation(summary = "Compute monthly VAT record from revenue transactions")
    public ResponseEntity<ApiResponse<TaxRecordDto>> computeVat(@RequestParam String period) {
        TaxRecord record = taxService.computeMonthlyVat(YearMonth.parse(period));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("VAT record computed", toDto(record)));
    }

    @GetMapping("/records")
    @Operation(summary = "List tax records by year")
    public ResponseEntity<ApiResponse<List<TaxRecordDto>>> listByYear(@RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(
                taxService.listByYear(year).stream().map(this::toDto).toList()));
    }

    @GetMapping("/records/{id}")
    @Operation(summary = "Get tax record by ID")
    public ResponseEntity<ApiResponse<TaxRecordDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toDto(taxService.getById(id))));
    }

    @PatchMapping("/records/{id}/submit")
    @Operation(summary = "Submit tax record to RRA")
    public ResponseEntity<ApiResponse<TaxRecordDto>> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Tax record submitted", toDto(taxService.submitTaxRecord(id))));
    }

    @PatchMapping("/records/{id}/amend")
    @Operation(summary = "Amend taxable amount on a record")
    public ResponseEntity<ApiResponse<TaxRecordDto>> amend(
            @PathVariable UUID id, @RequestParam BigDecimal newTaxableAmount) {
        return ResponseEntity.ok(ApiResponse.success(toDto(taxService.amendTaxRecord(id, newTaxableAmount))));
    }

    @PatchMapping("/records/{id}/pay")
    @Operation(summary = "Mark tax record as paid")
    public ResponseEntity<ApiResponse<TaxRecordDto>> markPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(toDto(taxService.markTaxAsPaid(id))));
    }

    // ── Tax Rules ─────────────────────────────────────────────────────────────

    @GetMapping("/rules")
    @Operation(summary = "List all tax rule configurations")
    public ResponseEntity<ApiResponse<List<TaxRuleDto>>> listRules() {
        return ResponseEntity.ok(ApiResponse.success(
                taxService.listRules().stream().map(this::toRuleDto).toList()));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new tax rule")
    public ResponseEntity<ApiResponse<TaxRuleDto>> createRule(@Valid @RequestBody CreateTaxRuleRequest req) {
        TaxRuleConfig rule = taxService.createRule(req.taxType(), req.rate(), req.description(),
                req.effectiveFrom(), req.effectiveTo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rule created", toRuleDto(rule)));
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    @GetMapping("/calculate")
    @Operation(summary = "Calculate tax on a given amount")
    public ResponseEntity<ApiResponse<TaxCalcResult>> calculate(
            @RequestParam BigDecimal amount, @RequestParam TaxType taxType) {
        BigDecimal tax = taxService.calculateTaxOnAmount(amount, taxType);
        return ResponseEntity.ok(ApiResponse.success(
                new TaxCalcResult(amount, taxType.name(), tax, amount.add(tax))));
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    private TaxRecordDto toDto(TaxRecord r) {
        return new TaxRecordDto(r.getId(), r.getPeriod().toString(), r.getTaxType().name(),
                r.getTaxableAmount(), r.getTaxRate(), r.getTaxDue(),
                r.getStatus().name(), r.getFiledAt());
    }

    private TaxRuleDto toRuleDto(TaxRuleConfig c) {
        return new TaxRuleDto(c.getId(), c.getTaxType().name(), c.getRate(), c.getDescription(),
                c.getEffectiveFrom(), c.getEffectiveTo(), c.getIsActive());
    }

    record TaxRecordDto(UUID id, String period, String taxType, BigDecimal taxableAmount,
                         BigDecimal taxRate, BigDecimal taxDue, String status, Instant filedAt) {}

    record TaxRuleDto(UUID id, String taxType, BigDecimal rate, String description,
                       LocalDate effectiveFrom, LocalDate effectiveTo, Boolean isActive) {}

    record TaxCalcResult(BigDecimal amount, String taxType, BigDecimal taxAmount, BigDecimal totalWithTax) {}

    record CreateTaxRuleRequest(
        @NotNull TaxType taxType,
        @NotNull BigDecimal rate,
        String description,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo
    ) {}
}
