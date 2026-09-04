package com.examly.springapp.controller;

import com.examly.springapp.model.Budget;
import com.examly.springapp.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final AuthHelper authHelper;

    public BudgetController(BudgetService budgetService, AuthHelper authHelper) {
        this.budgetService = budgetService;
        this.authHelper = authHelper;
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody Budget budget) {
        return ResponseEntity.ok(budgetService.saveBudget(authHelper.getCurrentUserId(), budget));
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getBudgets() {
        return ResponseEntity.ok(budgetService.getBudgets(authHelper.getCurrentUserId()));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<Budget>> getCurrentMonthSummary() {
        return ResponseEntity.ok(budgetService.getCurrentMonthBudgets(authHelper.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(@PathVariable Long id, @Valid @RequestBody Budget budget) {
        return ResponseEntity.ok(budgetService.updateBudget(id, authHelper.getCurrentUserId(), budget));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id, authHelper.getCurrentUserId());
        return ResponseEntity.ok("Budget deleted");
    }
}
