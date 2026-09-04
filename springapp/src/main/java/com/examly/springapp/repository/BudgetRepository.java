package com.examly.springapp.repository;

import com.examly.springapp.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    List<Budget> findByUserIdAndMonth(Long userId, LocalDate month);
    Optional<Budget> findByUserIdAndCategoryAndMonth(Long userId, String category, LocalDate month);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
}
