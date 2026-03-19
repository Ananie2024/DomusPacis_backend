package com.domuspacis.tax.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.finance.infrastructure.RevenueTransactionRepository;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import com.domuspacis.tax.domain.*;
import com.domuspacis.tax.infrastructure.TaxRecordRepository;
import com.domuspacis.tax.infrastructure.TaxRuleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaxCalculationService {

    private final TaxRecordRepository          taxRecordRepository;
    private final TaxRuleConfigRepository      ruleConfigRepository;
    private final RevenueTransactionRepository revenueRepo;

    public TaxRecord computeMonthlyVat(YearMonth period) {
        TaxRuleConfig rule = ruleConfigRepository
                .findActiveRule(TaxType.VAT, LocalDate.now())
                .orElseThrow(() -> new BusinessRuleViolationException("No active VAT rule found"));

        BigDecimal taxableAmount = revenueRepo
                .sumByDateRange(period.atDay(1), period.atEndOfMonth());

        BigDecimal taxDue = taxableAmount
                .multiply(rule.getRate())
                .setScale(2, RoundingMode.HALF_UP);

        TaxRecord record = TaxRecord.builder()
                .taxType(TaxType.VAT)
                .taxableAmount(taxableAmount)
                .taxRate(rule.getRate())
                .taxDue(taxDue)
                .status(TaxStatus.DRAFT)
                .build();
        record.setPeriod(period);

        TaxRecord saved = taxRecordRepository.save(record);
        log.info("VAT computed for {}: taxable={} rate={} due={}", period, taxableAmount, rule.getRate(), taxDue);
        return saved;
    }

    public BigDecimal calculateTaxOnAmount(BigDecimal amount, TaxType taxType) {
        TaxRuleConfig rule = ruleConfigRepository
                .findActiveRule(taxType, LocalDate.now())
                .orElseThrow(() -> new BusinessRuleViolationException("No active rule for: " + taxType));
        return amount.multiply(rule.getRate()).setScale(2, RoundingMode.HALF_UP);
    }

    @Audited("SUBMIT_TAX_RECORD")
    public TaxRecord submitTaxRecord(UUID recordId) {
        TaxRecord record = findById(recordId);
        if (record.getStatus() != TaxStatus.DRAFT)
            throw new BusinessRuleViolationException("Only DRAFT records can be submitted");
        record.setStatus(TaxStatus.SUBMITTED);
        record.setFiledAt(Instant.now());
        return taxRecordRepository.save(record);
    }

    @Audited("AMEND_TAX_RECORD")
    public TaxRecord amendTaxRecord(UUID recordId, BigDecimal newTaxableAmount) {
        TaxRecord record = findById(recordId);
        if (record.getStatus() == TaxStatus.PAID)
            throw new BusinessRuleViolationException("Paid tax records cannot be amended");
        BigDecimal newDue = newTaxableAmount.multiply(record.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
        record.setTaxableAmount(newTaxableAmount);
        record.setTaxDue(newDue);
        record.setStatus(TaxStatus.DRAFT);
        return taxRecordRepository.save(record);
    }

    @Audited("MARK_TAX_PAID")
    public TaxRecord markTaxAsPaid(UUID recordId) {
        TaxRecord record = findById(recordId);
        if (record.getStatus() != TaxStatus.SUBMITTED)
            throw new BusinessRuleViolationException("Only SUBMITTED records can be marked PAID");
        record.setStatus(TaxStatus.PAID);
        return taxRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public TaxRecord getById(UUID id) { return findById(id); }

    @Transactional(readOnly = true)
    public List<TaxRecord> listByPeriod(YearMonth period) {
        return taxRecordRepository.findByPeriodYearAndPeriodMonth(period.getYear(), period.getMonthValue());
    }

    @Transactional(readOnly = true)
    public List<TaxRecord> listByYear(int year) {
        return taxRecordRepository.findByPeriodYear(year);
    }

    @Transactional(readOnly = true)
    public List<TaxRecord> listByStatus(TaxStatus status) {
        return taxRecordRepository.findByStatus(status);
    }

    // ── TaxRuleConfig management ──────────────────────────────────────────────

    public TaxRuleConfig createRule(TaxType type, BigDecimal rate, String description,
                                     LocalDate from, LocalDate to) {
        TaxRuleConfig rule = TaxRuleConfig.builder()
                .taxType(type).rate(rate).description(description)
                .effectiveFrom(from).effectiveTo(to).isActive(true)
                .build();
        return ruleConfigRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<TaxRuleConfig> listRules() {
        return ruleConfigRepository.findAll();
    }

    private TaxRecord findById(UUID id) {
        return taxRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaxRecord", id));
    }
}
