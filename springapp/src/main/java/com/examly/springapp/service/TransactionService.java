package com.examly.springapp.service;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              BudgetRepository budgetRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.budgetRepository = budgetRepository;
    }

    @Transactional
    public Transaction addTransaction(Long accountId, Transaction transaction) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        transaction.setAccount(account);
        if (transaction.getTxnDate() == null) transaction.setTxnDate(LocalDateTime.now());
        Transaction saved = transactionRepository.save(transaction);

        // Update account balance
        BigDecimal amount = saved.getAmount() != null ? saved.getAmount() : BigDecimal.ZERO;
        BigDecimal current = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        if (saved.getType() == Transaction.TransactionType.DEBIT) {
            account.setBalance(current.subtract(amount));
        } else {
            account.setBalance(current.add(amount));
        }
        accountRepository.save(account);

        // Sync budget spentAmount for DEBIT transactions
        if (saved.getType() == Transaction.TransactionType.DEBIT && saved.getCategory() != null) {
            syncBudget(account.getUser().getId(), saved.getCategory(), saved.getAmount(), true);
        }
        return saved;
    }

    public List<Transaction> getTransactionsByUser(Long userId) {
        return transactionRepository.findByAccountUserIdOrderByTxnDateDesc(userId);
    }

    public List<Transaction> getTransactions(Long userId, String category, String merchant, LocalDate startDate, LocalDate endDate) {
        List<Transaction> list = transactionRepository.findByAccountUserIdOrderByTxnDateDesc(userId);
        return list.stream()
                .filter(t -> category == null || category.isBlank() || (t.getCategory() != null && t.getCategory().equalsIgnoreCase(category.trim())))
                .filter(t -> merchant == null || merchant.isBlank() || (t.getMerchant() != null && t.getMerchant().toLowerCase().contains(merchant.trim().toLowerCase())))
                .filter(t -> startDate == null || (t.getTxnDate() != null && !t.getTxnDate().toLocalDate().isBefore(startDate)))
                .filter(t -> endDate == null || (t.getTxnDate() != null && !t.getTxnDate().toLocalDate().isAfter(endDate)))
                .toList();
    }

    @Transactional
    public java.util.Map<String, Object> syncTransactions(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        for (Account a : accounts) {
            a.setLastSyncedAt(now);
            accountRepository.save(a);
        }
        return java.util.Map.of("message", "Transactions synchronized successfully", "syncedAccounts", accounts.size(), "timestamp", now.toString());
    }

    public List<Transaction> getTransactionsByAccount(Long accountId) {
        return transactionRepository.findByAccountIdOrderByTxnDateDesc(accountId);
    }

    @Transactional
    public void deleteTransaction(Long transactionId, Long userId) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!txn.getAccount().getUser().getId().equals(userId))
            throw new com.examly.springapp.exception.UnauthorisedAccessException("Access denied");

        // Reverse account balance
        Account account = txn.getAccount();
        BigDecimal amount = txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO;
        BigDecimal current = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        if (txn.getType() == Transaction.TransactionType.DEBIT) {
            account.setBalance(current.add(amount));
        } else {
            account.setBalance(current.subtract(amount));
        }
        accountRepository.save(account);

        // Reverse budget spentAmount for DEBIT transactions
        if (txn.getType() == Transaction.TransactionType.DEBIT && txn.getCategory() != null) {
            syncBudget(userId, txn.getCategory(), txn.getAmount(), false);
        }
        transactionRepository.delete(txn);
    }

    private void syncBudget(Long userId, String category, BigDecimal amount, boolean add) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        budgetRepository.findByUserIdAndCategoryAndMonth(userId, category, firstOfMonth)
                .ifPresent(budget -> {
                    BigDecimal current = budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO;
                    BigDecimal updated = add ? current.add(amount) : current.subtract(amount).max(BigDecimal.ZERO);
                    budget.setSpentAmount(updated);
                    budgetRepository.save(budget);
                });
    }
}
