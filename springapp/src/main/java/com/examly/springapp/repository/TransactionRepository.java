package com.examly.springapp.repository;

import com.examly.springapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdOrderByTxnDateDesc(Long accountId);
    List<Transaction> findByAccountUserIdOrderByTxnDateDesc(Long userId);
}
