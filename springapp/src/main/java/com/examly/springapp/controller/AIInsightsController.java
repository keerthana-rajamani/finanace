package com.examly.springapp.controller;

import com.examly.springapp.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIInsightsController {

    private final GeminiService geminiService;
    private final AuthHelper authHelper;

    public AIInsightsController(GeminiService geminiService, AuthHelper authHelper) {
        this.geminiService = geminiService;
        this.authHelper = authHelper;
    }

    @GetMapping("/insights")
    public ResponseEntity<Map<String, String>> getInsights() {
        Long userId = authHelper.getCurrentUserId();
        String insights = geminiService.getFinancialInsights(userId);
        return ResponseEntity.ok(Map.of("insights", insights));
    }

    @GetMapping("/spending-analysis")
    public ResponseEntity<Map<String, String>> getSpendingAnalysis() {
        Long userId = authHelper.getCurrentUserId();
        String analysis = geminiService.getSpendingAnalysis(userId);
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }

    @GetMapping("/budget-recommendations")
    public ResponseEntity<Map<String, String>> getBudgetRecommendations() {
        Long userId = authHelper.getCurrentUserId();
        String recommendations = geminiService.getBudgetRecommendations(userId);
        return ResponseEntity.ok(Map.of("recommendations", recommendations));
    }

    @GetMapping("/goal-advice")
    public ResponseEntity<Map<String, String>> getGoalAdvice() {
        Long userId = authHelper.getCurrentUserId();
        String advice = geminiService.getGoalAdvice(userId);
        return ResponseEntity.ok(Map.of("advice", advice));
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {
        Long userId = authHelper.getCurrentUserId();
        String question = request.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question cannot be empty"));
        }
        String answer = geminiService.askQuestion(userId, question);
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
