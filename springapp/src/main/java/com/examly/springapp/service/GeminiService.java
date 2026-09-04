package com.examly.springapp.service;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;

    public GeminiService(AccountRepository accountRepository,
                         TransactionRepository transactionRepository,
                         BudgetRepository budgetRepository,
                         GoalRepository goalRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
    }

    public String getFinancialInsights(Long userId) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Based on the following financial data, provide 5 concise, actionable insights and tips to improve financial health. Be specific with numbers. Format as numbered points.\n\n" + context;
        return callGemini(prompt);
    }

    public String getSpendingAnalysis(Long userId) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Analyze the spending patterns from this financial data and provide a brief spending analysis with 3 key observations and 2 recommendations. Be concise.\n\n" + context;
        return callGemini(prompt);
    }

    public String getBudgetRecommendations(Long userId) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Based on this financial data, suggest optimal budget allocations using the 50-30-20 rule. Provide specific amounts for each category. Be concise.\n\n" + context;
        return callGemini(prompt);
    }

    public String getGoalAdvice(Long userId) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Review these financial goals and provide specific advice on how to achieve them faster. Include monthly savings recommendations. Be concise.\n\n" + context;
        return callGemini(prompt);
    }

    public String askQuestion(Long userId, String question) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Here is the user's financial data:\n\n" + context + "\n\nUser question: " + question + "\n\nProvide a helpful, specific answer based on their actual financial data.";
        return callGemini(prompt);
    }

    private String buildFinancialContext(Long userId) {
        StringBuilder sb = new StringBuilder();

        List<Account> accounts = accountRepository.findByUserId(userId);
        BigDecimal totalBalance = accounts.stream()
                .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        sb.append("=== FINANCIAL SUMMARY ===\n");
        sb.append("Total Bank Balance: ₹").append(totalBalance).append("\n");
        sb.append("Linked Accounts: ").append(accounts.size()).append("\n");

        accounts.forEach(a -> sb.append("  - ").append(a.getBankName())
                .append(" (").append(a.getAccountType()).append("): ₹")
                .append(a.getBalance()).append("\n"));

        List<Transaction> transactions = transactionRepository.findByAccountUserIdOrderByTxnDateDesc(userId);
        sb.append("\n=== RECENT TRANSACTIONS (last 20) ===\n");
        transactions.stream().limit(20).forEach(t ->
                sb.append("  ").append(t.getType()).append(" ₹").append(t.getAmount())
                        .append(" | ").append(t.getCategory() != null ? t.getCategory() : "Uncategorized")
                        .append(" | ").append(t.getMerchant() != null ? t.getMerchant() : "")
                        .append("\n"));

        Map<String, BigDecimal> categorySpend = new HashMap<>();
        transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.DEBIT)
                .forEach(t -> {
                    String cat = t.getCategory() != null ? t.getCategory() : "Other";
                    categorySpend.merge(cat, t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO, BigDecimal::add);
                });

        sb.append("\n=== SPENDING BY CATEGORY ===\n");
        categorySpend.forEach((cat, amt) -> sb.append("  ").append(cat).append(": ₹").append(amt).append("\n"));

        List<Budget> budgets = budgetRepository.findByUserId(userId);
        sb.append("\n=== BUDGETS ===\n");
        budgets.forEach(b -> sb.append("  ").append(b.getCategory())
                .append(": Spent ₹").append(b.getSpentAmount())
                .append(" of ₹").append(b.getBudgetAmount()).append("\n"));

        List<Goal> goals = goalRepository.findByUserId(userId);
        sb.append("\n=== FINANCIAL GOALS ===\n");
        goals.forEach(g -> sb.append("  ").append(g.getName())
                .append(": ₹").append(g.getCurrentAmount())
                .append(" / ₹").append(g.getTargetAmount())
                .append(" by ").append(g.getTargetDate())
                .append(" [").append(g.getPriority()).append("]\n"));

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        try {
            WebClient client = WebClient.create();

            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> body = Map.of("contents", List.of(content));

            Map<String, Object> response = client.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                    if (!parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            return "Unable to generate insights at this time. Please try again.";
        } catch (Exception e) {
            return "AI insights temporarily unavailable: " + e.getMessage();
        }
    }
}
