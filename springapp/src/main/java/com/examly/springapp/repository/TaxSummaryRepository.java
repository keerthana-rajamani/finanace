package com.examly.springapp.repository;

import com.examly.springapp.model.TaxSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaxSummaryRepository extends JpaRepository<TaxSummary, Long> {
    List<TaxSummary> findByUserId(Long userId);
    Optional<TaxSummary> findByUserIdAndFinancialYear(Long userId, int financialYear);
    Optional<TaxSummary> findByIdAndUserId(Long id, Long userId);
}
