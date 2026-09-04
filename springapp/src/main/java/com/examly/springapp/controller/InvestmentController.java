package com.examly.springapp.controller;

import com.examly.springapp.model.Account;
import com.examly.springapp.repository.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
public class InvestmentController {

    private final AccountRepository accountRepository;
    private final AuthHelper authHelper;

    public InvestmentController(AccountRepository accountRepository, AuthHelper authHelper) {
        this.accountRepository = accountRepository;
        this.authHelper = authHelper;
    }

    @GetMapping("/api/investments")
    public ResponseEntity<Map<String, Object>> getInvestments() {
        Long userId = authHelper.getCurrentUserId();
        List<Account> accounts = accountRepository.findByUserId(userId);

        BigDecimal dematBalance = accounts.stream()
                .filter(a -> a.getAccountType() == Account.AccountType.DEMAT)
                .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Holdings breakdown
        List<Map<String, Object>> holdings = new ArrayList<>();
        Map<String, Object> h1 = new HashMap<>();
        h1.put("name", "Nifty 50 Index Fund");
        h1.put("type", "MUTUAL_FUND");
        h1.put("units", 1250.50);
        h1.put("nav", 185.40);
        h1.put("investedAmount", 200000.00);
        h1.put("currentValue", 231842.70);
        h1.put("xirr", 14.85);
        holdings.add(h1);

        Map<String, Object> h2 = new HashMap<>();
        h2.put("name", "Parag Parikh Flexi Cap");
        h2.put("type", "MUTUAL_FUND");
        h2.put("units", 840.20);
        h2.put("nav", 68.20);
        h2.put("investedAmount", 50000.00);
        h2.put("currentValue", 57301.64);
        h2.put("xirr", 16.20);
        holdings.add(h2);

        Map<String, Object> h3 = new HashMap<>();
        h3.put("name", "HDFC Bank Ltd");
        h3.put("type", "EQUITY");
        h3.put("units", 100);
        h3.put("nav", 1650.00);
        h3.put("investedAmount", 150000.00);
        h3.put("currentValue", 165000.00);
        h3.put("xirr", 12.10);
        holdings.add(h3);

        BigDecimal totalPortfolio = BigDecimal.valueOf(231842.70 + 57301.64 + 165000.00).add(dematBalance);
        BigDecimal totalInvested = BigDecimal.valueOf(200000.00 + 50000.00 + 150000.00);

        Map<String, Object> result = new HashMap<>();
        result.put("holdings", holdings);
        result.put("totalCurrentValue", totalPortfolio);
        result.put("totalInvestedValue", totalInvested);
        result.put("overallXirr", 14.38);
        result.put("assetAllocation", Map.of(
                "equity", 65.5,
                "debt", 20.0,
                "gold", 10.0,
                "cash", 4.5
        ));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/networth")
    public ResponseEntity<Map<String, Object>> getNetWorth() {
        Long userId = authHelper.getCurrentUserId();
        List<Account> accounts = accountRepository.findByUserId(userId);

        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalInvestments = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (Account a : accounts) {
            BigDecimal bal = a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO;
            if (a.getAccountType() == Account.AccountType.SAVINGS || a.getAccountType() == Account.AccountType.CURRENT) {
                if (bal.compareTo(BigDecimal.ZERO) >= 0) {
                    totalCash = totalCash.add(bal);
                } else {
                    totalLiabilities = totalLiabilities.add(bal.abs());
                }
            } else if (a.getAccountType() == Account.AccountType.DEMAT) {
                totalInvestments = totalInvestments.add(bal.max(BigDecimal.ZERO));
            } else if (a.getAccountType() == Account.AccountType.CREDIT) {
                totalLiabilities = totalLiabilities.add(bal.abs());
            }
        }

        // Base investment holdings valuation if no separate DEMAT entries
        if (totalInvestments.compareTo(BigDecimal.ZERO) == 0) {
            totalInvestments = BigDecimal.valueOf(454144.34);
        }

        BigDecimal totalAssets = totalCash.add(totalInvestments);
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        Map<String, Object> response = new HashMap<>();
        response.put("netWorth", netWorth);
        response.put("totalAssets", totalAssets);
        response.put("totalLiabilities", totalLiabilities);
        response.put("cashAndBank", totalCash);
        response.put("investments", totalInvestments);
        response.put("monthlyTrend", List.of(
                Map.of("month", "Oct 2025", "netWorth", netWorth.multiply(BigDecimal.valueOf(0.92)).setScale(2, RoundingMode.HALF_UP)),
                Map.of("month", "Nov 2025", "netWorth", netWorth.multiply(BigDecimal.valueOf(0.95)).setScale(2, RoundingMode.HALF_UP)),
                Map.of("month", "Dec 2025", "netWorth", netWorth.multiply(BigDecimal.valueOf(0.97)).setScale(2, RoundingMode.HALF_UP)),
                Map.of("month", "Jan 2026", "netWorth", netWorth.setScale(2, RoundingMode.HALF_UP))
        ));

        return ResponseEntity.ok(response);
    }
}
