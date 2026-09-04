package com.examly.springapp.service;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public GoalService(GoalRepository goalRepository, UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    public Goal createGoal(Long userId, Goal goal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (goal.getCurrentAmount() == null) goal.setCurrentAmount(BigDecimal.ZERO);
        if (goal.getTargetAmount().compareTo(goal.getCurrentAmount()) <= 0)
            throw new IllegalArgumentException("Target amount must exceed current savings");
        goal.setUser(user);
        goal.setStatus(Goal.Status.ACTIVE);
        return goalRepository.save(goal);
    }

    public List<Goal> getGoals(Long userId) {
        return goalRepository.findByUserId(userId);
    }

    public Goal updateGoal(Long goalId, Long userId, Goal updated) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        BigDecimal current = updated.getCurrentAmount() != null ? updated.getCurrentAmount() : BigDecimal.ZERO;
        if (updated.getTargetAmount().compareTo(current) <= 0)
            throw new IllegalArgumentException("Target amount must exceed current savings");
        goal.setName(updated.getName());
        goal.setTargetAmount(updated.getTargetAmount());
        goal.setTargetDate(updated.getTargetDate());
        goal.setCurrentAmount(current);
        goal.setPriority(updated.getPriority() != null ? updated.getPriority() : goal.getPriority());
        goal.setStatus(updated.getStatus() != null ? updated.getStatus() : goal.getStatus());
        return goalRepository.save(goal);
    }

    public void deleteGoal(Long goalId, Long userId) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        goalRepository.delete(goal);
    }
}
