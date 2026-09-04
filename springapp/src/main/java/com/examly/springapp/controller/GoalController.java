package com.examly.springapp.controller;

import com.examly.springapp.model.Goal;
import com.examly.springapp.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final AuthHelper authHelper;

    public GoalController(GoalService goalService, AuthHelper authHelper) {
        this.goalService = goalService;
        this.authHelper = authHelper;
    }

    @PostMapping
    public ResponseEntity<Goal> createGoal(@Valid @RequestBody Goal goal) {
        return ResponseEntity.ok(goalService.createGoal(authHelper.getCurrentUserId(), goal));
    }

    @GetMapping
    public ResponseEntity<List<Goal>> getGoals() {
        return ResponseEntity.ok(goalService.getGoals(authHelper.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Goal> updateGoal(@PathVariable Long id, @Valid @RequestBody Goal goal) {
        return ResponseEntity.ok(goalService.updateGoal(id, authHelper.getCurrentUserId(), goal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id, authHelper.getCurrentUserId());
        return ResponseEntity.ok("Goal deleted");
    }
}
