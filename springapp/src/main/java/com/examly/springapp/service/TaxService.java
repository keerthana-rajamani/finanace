package com.examly.springapp.service;

import com.examly.springapp.exception.*;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class TaxService {

    private final TaxSummaryRepository taxSummaryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public TaxService(TaxSummaryRepository taxSummaryRepository,
                      UserRepository userRepository,
                      TransactionRepository transactionRepository) {
        this.taxSummaryRepository = taxSummaryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public TaxSummary getOrComputeTaxSummary(Long userId, int financialYear) {
        return taxSummaryRepository.findByUserIdAndFinancialYear(userId, financialYear)
                .orElseGet(() -> computeAndSave(userId, financialYear));
    }

    public List<TaxSummary> getAllTaxSummaries(Long userId) {
        return taxSummaryRepository.findByUserId(userId);
    }

    public TaxSummary saveTaxSummary(Long userId, TaxSummary taxSummary) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        taxSummary.setUser(user);
        taxSummary.setTaxableIncome(computeTaxableIncome(taxSummary));
        taxSummary.setEstimatedTax(computeEstimatedTax(taxSummary.getTaxableIncome()));
        return taxSummaryRepository.save(taxSummary);
    }

    public TaxSummary updateTaxSummary(Long taxId, Long userId, TaxSummary updated) {
        TaxSummary existing = taxSummaryRepository.findByIdAndUserId(taxId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax summary not found"));
        existing.setTotalIncome(updated.getTotalIncome());
        existing.setLtcg(updated.getLtcg());
        existing.setStcg(updated.getStcg());
        existing.setInterestIncome(updated.getInterestIncome());
        existing.setSection80c(updated.getSection80c());
        existing.setSection80d(updated.getSection80d());
        existing.setHraExemption(updated.getHraExemption());
        existing.setAdvanceTaxPaid(updated.getAdvanceTaxPaid());
        existing.setTaxableIncome(computeTaxableIncome(existing));
        existing.setEstimatedTax(computeEstimatedTax(existing.getTaxableIncome()));
        return taxSummaryRepository.save(existing);
    }

    private TaxSummary computeAndSave(Long userId, int financialYear) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Transaction> transactions = transactionRepository.findByAccountUserIdOrderByTxnDateDesc(userId);

        LocalDate fyStart = LocalDate.of(financialYear, 4, 1);
        LocalDate fyEnd = LocalDate.of(financialYear + 1, 3, 31);

        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.CREDIT)
                .filter(t -> t.getTxnDate() != null &&
                        !t.getTxnDate().toLocalDate().isBefore(fyStart) &&
                        !t.getTxnDate().toLocalDate().isAfter(fyEnd))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal interestIncome = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.CREDIT)
                .filter(t -> "Interest".equalsIgnoreCase(t.getCategory()))
                .filter(t -> t.getTxnDate() != null &&
                        !t.getTxnDate().toLocalDate().isBefore(fyStart) &&
                        !t.getTxnDate().toLocalDate().isAfter(fyEnd))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TaxSummary summary = new TaxSummary();
        summary.setUser(user);
        summary.setFinancialYear(financialYear);
        summary.setTotalIncome(totalIncome);
        summary.setInterestIncome(interestIncome);
        summary.setLtcg(BigDecimal.ZERO);
        summary.setStcg(BigDecimal.ZERO);
        summary.setSection80c(BigDecimal.ZERO);
        summary.setSection80d(BigDecimal.ZERO);
        summary.setHraExemption(BigDecimal.ZERO);
        summary.setAdvanceTaxPaid(BigDecimal.ZERO);
        summary.setTaxableIncome(computeTaxableIncome(summary));
        summary.setEstimatedTax(computeEstimatedTax(summary.getTaxableIncome()));
        return taxSummaryRepository.save(summary);
    }

    private BigDecimal computeTaxableIncome(TaxSummary t) {
        BigDecimal deductions = safe(t.getSection80c()).min(new BigDecimal("150000"))
                .add(safe(t.getSection80d()).min(new BigDecimal("25000")))
                .add(safe(t.getHraExemption()));
        BigDecimal taxable = safe(t.getTotalIncome())
                .add(safe(t.getLtcg()))
                .add(safe(t.getStcg()))
                .add(safe(t.getInterestIncome()))
                .subtract(deductions);
        return taxable.max(BigDecimal.ZERO);
    }

    private BigDecimal computeEstimatedTax(BigDecimal taxableIncome) {
        // New tax regime slabs FY2024-25
        BigDecimal tax = BigDecimal.ZERO;
        long income = taxableIncome.longValue();
        if (income <= 300000) return BigDecimal.ZERO;
        if (income <= 600000) tax = BigDecimal.valueOf((income - 300000) * 0.05);
        else if (income <= 900000) tax = BigDecimal.valueOf(15000 + (income - 600000) * 0.10);
        else if (income <= 1200000) tax = BigDecimal.valueOf(45000 + (income - 900000) * 0.15);
        else if (income <= 1500000) tax = BigDecimal.valueOf(90000 + (income - 1200000) * 0.20);
        else tax = BigDecimal.valueOf(150000 + (income - 1500000) * 0.30);
        // Add 4% cess
        return tax.multiply(new BigDecimal("1.04")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
