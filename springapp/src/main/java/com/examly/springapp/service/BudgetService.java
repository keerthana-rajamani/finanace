package com.examly.springapp.service;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository, UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    public Budget saveBudget(Long userId, Budget budget) {
        if (budget.getBudgetAmount() == null || budget.getBudgetAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new com.examly.springapp.exception.BudgetValidationException("Budget amount must be a positive number");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        LocalDate month = budget.getMonth() != null ? budget.getMonth().withDayOfMonth(1) : LocalDate.now().withDayOfMonth(1);

        Budget target = budgetRepository.findByUserIdAndCategoryAndMonth(userId, budget.getCategory(), month)
                .orElse(budget);
        target.setUser(user);
        target.setCategory(budget.getCategory());
        target.setMonth(month);
        target.setBudgetAmount(budget.getBudgetAmount());
        target.setAlertAtPercent(budget.getAlertAtPercent());
        target.setCarryForward(budget.isCarryForward());
        return budgetRepository.save(target);
    }

    public List<Budget> getBudgets(Long userId) {
        return budgetRepository.findByUserId(userId);
    }

    public List<Budget> getCurrentMonthBudgets(Long userId) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        return budgetRepository.findByUserIdAndMonth(userId, firstOfMonth);
    }

    public Budget updateBudget(Long budgetId, Long userId, Budget updated) {
        if (updated.getBudgetAmount() == null || updated.getBudgetAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new com.examly.springapp.exception.BudgetValidationException("Budget amount must be a positive number");
        }
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budget.setBudgetAmount(updated.getBudgetAmount());
        budget.setAlertAtPercent(updated.getAlertAtPercent());
        budget.setCarryForward(updated.isCarryForward());
        return budgetRepository.save(budget);
    }

    public void deleteBudget(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }
}
