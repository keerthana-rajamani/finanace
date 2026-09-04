package com.examly.springapp.controller;

import com.examly.springapp.model.Transaction;
import com.examly.springapp.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthHelper authHelper;

    public TransactionController(TransactionService transactionService, AuthHelper authHelper) {
        this.transactionService = transactionService;
        this.authHelper = authHelper;
    }

    @PostMapping("/account/{accountId}")
    public ResponseEntity<Transaction> addTransaction(@PathVariable Long accountId,
                                                       @RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.addTransaction(accountId, transaction));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncTransactions() {
        return ResponseEntity.ok(transactionService.syncTransactions(authHelper.getCurrentUserId()));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {
        if (category != null || merchant != null || startDate != null || endDate != null) {
            return ResponseEntity.ok(transactionService.getTransactions(authHelper.getCurrentUserId(), category, merchant, startDate, endDate));
        }
        return ResponseEntity.ok(transactionService.getTransactionsByUser(authHelper.getCurrentUserId()));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Transaction>> getByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id, authHelper.getCurrentUserId());
        return ResponseEntity.ok("Transaction deleted");
    }
}
