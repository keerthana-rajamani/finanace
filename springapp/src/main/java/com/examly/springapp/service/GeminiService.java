package com.examly.springapp.service;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private volatile String customApiKey = null;

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

    public void setCustomApiKey(String key) {
        if (key != null && !key.trim().isEmpty()) {
            this.customApiKey = key.trim();
        } else {
            this.customApiKey = null;
        }
    }

    public String getEffectiveApiKey() {
        if (customApiKey != null && !customApiKey.isBlank()) {
            return customApiKey;
        }
        return apiKey;
    }

    public boolean isRealApiKeyConfigured() {
        String key = getEffectiveApiKey();
        return key != null && !key.isBlank() 
                && !key.contains("DemoKey") 
                && !key.equals("test-key") 
                && key.startsWith("AIzaSy")
                && key.length() >= 30;
    }

    public Map<String, Object> getStatus() {
        boolean live = isRealApiKeyConfigured();
        return Map.of(
                "isGeminiLive", live,
                "engine", live ? "Google Gemini 2.0 Flash" : "Smart Financial Intelligence Engine",
                "customKeyActive", customApiKey != null
        );
    }

    public String getFinancialInsights(Long userId) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Based on the following financial data, provide 5 concise, actionable insights and tips to improve financial health. Be specific with numbers. Format as numbered points.\n\n" + context;
        return executeWithFallback(prompt, () -> generateSmartInsights(userId));
    }

    public String getSpendingAnalysis(Long userId) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Analyze the spending patterns from this financial data and provide a brief spending analysis with 3 key observations and 2 recommendations. Be concise.\n\n" + context;
        return executeWithFallback(prompt, () -> generateSmartSpendingAnalysis(userId));
    }

    public String getBudgetRecommendations(Long userId) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Based on this financial data, suggest optimal budget allocations using the 50-30-20 rule. Provide specific amounts for each category. Be concise.\n\n" + context;
        return executeWithFallback(prompt, () -> generateSmartBudgetRecommendations(userId));
    }

    public String getGoalAdvice(Long userId) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Review these financial goals and provide specific advice on how to achieve them faster. Include monthly savings recommendations. Be concise.\n\n" + context;
        return executeWithFallback(prompt, () -> generateSmartGoalAdvice(userId));
    }

    public String askQuestion(Long userId, String question) {
        String context = buildFinancialContext(userId);
        String prompt = "You are a personal finance advisor. Here is the user's financial data:\n\n" + context + "\n\nUser question: " + question + "\n\nProvide a helpful, specific answer based on their actual financial data.";
        return executeWithFallback(prompt, () -> generateSmartAnswer(userId, question));
    }

    private String executeWithFallback(String prompt, Supplier<String> fallbackSupplier) {
        if (isRealApiKeyConfigured()) {
            try {
                String geminiResponse = callGemini(prompt);
                if (geminiResponse != null && !geminiResponse.isBlank() && !geminiResponse.startsWith("AI insights temporarily unavailable")) {
                    return geminiResponse;
                }
            } catch (Exception e) {
                System.err.println("Gemini API call failed: " + e.getMessage() + ". Switching to Smart Financial Intelligence Engine.");
            }
        }
        return fallbackSupplier.get();
    }

    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        try {
            WebClient client = WebClient.builder().build();

            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> body = Map.of("contents", List.of(content));

            Map<String, Object> response = client.post()
                    .uri(apiUrl + "?key=" + getEffectiveApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                    if (contentMap != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Gemini call exception: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // SMART FINANCIAL INTELLIGENCE ENGINE (Deterministic Local Advisor)
    // =========================================================================

    public String generateSmartInsights(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        BigDecimal totalBalance = accounts.stream()
                .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Transaction> transactions = transactionRepository.findByAccountUserIdOrderByTxnDateDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> currentMonthTxns = filterCurrentMonthTransactions(transactions, now);

        BigDecimal monthSpend = currentMonthTxns.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.DEBIT)
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthIncome = currentMonthTxns.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.CREDIT)
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> categorySpend = getCategorySpending(currentMonthTxns);
        String topCategory = categorySpend.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("General Expenses");
        BigDecimal topAmount = categorySpend.getOrDefault(topCategory, BigDecimal.ZERO);

        List<Budget> budgets = budgetRepository.findByUserId(userId);
        Optional<Budget> alertBudget = budgets.stream()
                .filter(b -> b.getBudgetAmount() != null && b.getSpentAmount() != null &&
                        b.getSpentAmount().compareTo(b.getBudgetAmount().multiply(new BigDecimal("0.80"))) >= 0)
                .findFirst();

        List<Goal> goals = goalRepository.findByUserId(userId);
        Optional<Goal> activeGoal = goals.stream().findFirst();

        StringBuilder sb = new StringBuilder();
        sb.append("1. **Liquidity Health**: Your total bank balance across ")
                .append(accounts.size()).append(" linked account(s) is ₹")
                .append(formatAmount(totalBalance)).append(".\n");

        sb.append("2. **Monthly Cash Flow**: You have recorded ₹")
                .append(formatAmount(monthSpend)).append(" in expenses");
        if (monthIncome.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" against ₹").append(formatAmount(monthIncome)).append(" in income (Net Cash Flow: ₹")
                    .append(formatAmount(monthIncome.subtract(monthSpend))).append(").\n");
        } else {
            sb.append(" this month.\n");
        }

        sb.append("3. **Top Spending Driver**: Your highest expenditure category is **")
                .append(topCategory).append("** at ₹").append(formatAmount(topAmount));
        if (monthSpend.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pct = topAmount.multiply(new BigDecimal("100")).divide(monthSpend, 1, RoundingMode.HALF_UP);
            sb.append(" (").append(pct).append("% of monthly spending)");
        }
        sb.append(".\n");

        if (alertBudget.isPresent()) {
            Budget b = alertBudget.get();
            BigDecimal pct = b.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0 ?
                    b.getSpentAmount().multiply(new BigDecimal("100")).divide(b.getBudgetAmount(), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            sb.append("4. **Budget Alert**: Your **").append(b.getCategory())
                    .append("** budget is at ").append(pct).append("% utilization (Spent ₹")
                    .append(formatAmount(b.getSpentAmount())).append(" of ₹")
                    .append(formatAmount(b.getBudgetAmount())).append("). Monitor upcoming transactions.\n");
        } else if (!budgets.isEmpty()) {
            sb.append("4. **Budget Discipline**: Excellent job! All of your active category budgets are well within planned spending limits.\n");
        } else {
            sb.append("4. **Budget Tip**: Set category budgets for high-frequency expenses like Food and Shopping to prevent unexpected month-end deficits.\n");
        }

        if (activeGoal.isPresent()) {
            Goal g = activeGoal.get();
            BigDecimal pct = (g.getTargetAmount() != null && g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) ?
                    g.getCurrentAmount().multiply(new BigDecimal("100")).divide(g.getTargetAmount(), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            sb.append("5. **Goal Progress**: You have saved ₹").append(formatAmount(g.getCurrentAmount()))
                    .append(" towards **").append(g.getName()).append("** (").append(pct)
                    .append("% achieved). Target deadline: ").append(g.getTargetDate()).append(".\n");
        } else {
            sb.append("5. **Savings Milestone**: Create a defined Goal (such as an Emergency Fund or Vacation) in the Goals section to start automated target tracking.\n");
        }

        return sb.toString();
    }

    public String generateSmartSpendingAnalysis(Long userId) {
        List<Transaction> transactions = transactionRepository.findByAccountUserIdOrderByTxnDateDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> currentMonthTxns = filterCurrentMonthTransactions(transactions, now);

        List<Transaction> debits = currentMonthTxns.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.DEBIT)
                .collect(Collectors.toList());

        BigDecimal totalSpend = debits.stream()
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> categorySpend = getCategorySpending(currentMonthTxns);
        String topCategory = categorySpend.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("General");
        BigDecimal topCatAmount = categorySpend.getOrDefault(topCategory, BigDecimal.ZERO);

        BigDecimal avgTicket = debits.isEmpty() ? BigDecimal.ZERO :
                totalSpend.divide(new BigDecimal(debits.size()), 2, RoundingMode.HALF_UP);

        StringBuilder sb = new StringBuilder();
        sb.append("=== 📊 SPENDING OBSERVATIONS ===\n");
        sb.append("1. Total debits recorded: ₹").append(formatAmount(totalSpend))
                .append(" across ").append(debits.size()).append(" transaction(s) this month.\n");
        sb.append("2. Dominant expenditure: **").append(topCategory).append("** represents ₹")
                .append(formatAmount(topCatAmount));
        if (totalSpend.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pct = topCatAmount.multiply(new BigDecimal("100")).divide(totalSpend, 1, RoundingMode.HALF_UP);
            sb.append(" (").append(pct).append("% of total spend)");
        }
        sb.append(".\n");
        sb.append("3. Average expense per transaction: ₹").append(formatAmount(avgTicket)).append(".\n\n");

        sb.append("=== 💡 STRATEGIC RECOMMENDATIONS ===\n");
        sb.append("1. **Cap ").append(topCategory).append(" Spend**: Setting a weekly limit on ")
                .append(topCategory).append(" could save you up to 15-20% per month.\n");
        sb.append("2. **Implement Automated Sweeps**: Route surplus bank balances into high-yield savings or mutual fund SIPs immediately following salary credits.");

        return sb.toString();
    }

    public String generateSmartBudgetRecommendations(Long userId) {
        List<Transaction> transactions = transactionRepository.findByAccountUserIdOrderByTxnDateDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> currentMonthTxns = filterCurrentMonthTransactions(transactions, now);

        BigDecimal monthIncome = currentMonthTxns.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.CREDIT)
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthSpend = currentMonthTxns.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.DEBIT)
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal baseAmount = monthIncome.compareTo(BigDecimal.ZERO) > 0 ? monthIncome :
                (monthSpend.compareTo(BigDecimal.ZERO) > 0 ? monthSpend.multiply(new BigDecimal("1.25")) : new BigDecimal("50000"));

        BigDecimal needs = baseAmount.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal wants = baseAmount.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal savings = baseAmount.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);

        StringBuilder sb = new StringBuilder();
        sb.append("=== 💰 50-30-20 BUDGET RECOMMENDATIONS ===\n");
        sb.append("Based on your monthly cashflow base of ₹").append(formatAmount(baseAmount)).append(":\n\n");

        sb.append("1. **Needs (50% — ₹").append(formatAmount(needs)).append(")**:\n");
        sb.append("   - Allocate for essentials: Housing/Rent, Groceries, Utilities, Healthcare, and Commute.\n");
        sb.append("   - Suggested Grocery/Food Budget: ₹").append(formatAmount(needs.multiply(new BigDecimal("0.40")))).append("\n");
        sb.append("   - Suggested Utilities Budget: ₹").append(formatAmount(needs.multiply(new BigDecimal("0.20")))).append("\n\n");

        sb.append("2. **Wants (30% — ₹").append(formatAmount(wants)).append(")**:\n");
        sb.append("   - Allocate for lifestyle: Dining Out, Shopping, Entertainment, and Subscriptions.\n");
        sb.append("   - Keep dining & leisure below ₹").append(formatAmount(wants.multiply(new BigDecimal("0.50")))).append(".\n\n");

        sb.append("3. **Savings & Investments (20% — ₹").append(formatAmount(savings)).append(")**:\n");
        sb.append("   - Prioritize emergency fund until 3–6 months of living expenses are secured.\n");
        sb.append("   - Set up an automated recurring deposit or index fund SIP for ₹").append(formatAmount(savings)).append("/month.");

        return sb.toString();
    }

    public String generateSmartGoalAdvice(Long userId) {
        List<Goal> goals = goalRepository.findByUserId(userId);
        if (goals.isEmpty()) {
            return "=== 🎯 GOAL ADVICE ===\n" +
                    "You haven't established any savings goals yet.\n\n" +
                    "1. **Emergency Cushion**: We recommend creating an Emergency Fund goal with a target of 3 to 6 months of expenses.\n" +
                    "2. **Short-Term Targets**: Set up explicit goals for upcoming purchases or travel to stay motivated and avoid debt.\n" +
                    "3. **Use the Goals Section**: Click 'Add Goal' to specify a target amount and deadline.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 🎯 GOAL PROGRESS & ADVICE ===\n\n");

        for (int i = 0; i < goals.size(); i++) {
            Goal g = goals.get(i);
            BigDecimal target = g.getTargetAmount() != null ? g.getTargetAmount() : BigDecimal.ZERO;
            BigDecimal current = g.getCurrentAmount() != null ? g.getCurrentAmount() : BigDecimal.ZERO;
            BigDecimal remaining = target.subtract(current);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

            BigDecimal pct = target.compareTo(BigDecimal.ZERO) > 0 ?
                    current.multiply(new BigDecimal("100")).divide(target, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            long monthsRemaining = 1;
            if (g.getTargetDate() != null) {
                long m = ChronoUnit.MONTHS.between(LocalDate.now(), g.getTargetDate());
                monthsRemaining = Math.max(1, m);
            }

            BigDecimal monthlyTarget = remaining.divide(new BigDecimal(monthsRemaining), 2, RoundingMode.HALF_UP);

            sb.append((i + 1)).append(". **").append(g.getName()).append("** [").append(g.getPriority()).append(" Priority]\n");
            sb.append("   - Saved: ₹").append(formatAmount(current)).append(" / ₹").append(formatAmount(target))
                    .append(" (").append(pct).append("% achieved)\n");
            sb.append("   - Target Date: ").append(g.getTargetDate()).append(" (approx. ")
                    .append(monthsRemaining).append(" month(s) left)\n");
            sb.append("   - **Monthly Contribution Needed**: ₹").append(formatAmount(monthlyTarget)).append("/month.\n\n");
        }

        sb.append("💡 **Advisor Strategy**: Automate your monthly savings right after income is deposited so you consistently fund your goals before discretionary spending.");
        return sb.toString();
    }

    public String generateSmartAnswer(Long userId, String question) {
        String q = (question != null) ? question.toLowerCase().trim() : "";
        List<Transaction> allTransactions = transactionRepository.findByAccountUserIdOrderByTxnDateDesc(userId);
        List<Account> accounts = accountRepository.findByUserId(userId);
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        List<Goal> goals = goalRepository.findByUserId(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Transaction> currentMonthTxns = filterCurrentMonthTransactions(allTransactions, now);

        // 1. Food / Dining / Groceries spending
        if (q.contains("food") || q.contains("dining") || q.contains("restaurant") || 
            q.contains("swiggy") || q.contains("zomato") || q.contains("groceries") || 
            q.contains("grocery") || q.contains("eat") || q.contains("meal")) {
            
            List<Transaction> foodTxns = allTransactions.stream()
                    .filter(t -> t.getType() == Transaction.TransactionType.DEBIT)
                    .filter(t -> isFoodTransaction(t))
                    .collect(Collectors.toList());

            // Check if asking specifically about this month
            boolean thisMonthSpecific = q.contains("month");
            List<Transaction> matchedTxns = thisMonthSpecific ?
                    foodTxns.stream().filter(t -> isSameMonth(t.getTxnDate(), now)).collect(Collectors.toList()) :
                    foodTxns;

            // If this month had 0 but all-time had some, include note
            if (matchedTxns.isEmpty() && !foodTxns.isEmpty() && thisMonthSpecific) {
                BigDecimal allTimeFood = foodTxns.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                return "Based on your transaction records:\n" +
                        "1. **Food Spending This Month**: ₹0.00 across 0 transactions.\n" +
                        "2. **All-Time Food Spending**: ₹" + formatAmount(allTimeFood) + " across " + foodTxns.size() + " recorded transaction(s).\n" +
                        "3. **Recommendation**: Keep logging your daily grocery and dining expenses to ensure your monthly food budget remains accurate.";
            }

            BigDecimal totalFood = matchedTxns.stream()
                    .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Optional<Budget> foodBudget = budgets.stream()
                    .filter(b -> b.getCategory() != null && b.getCategory().equalsIgnoreCase("Food"))
                    .findFirst();

            StringBuilder sb = new StringBuilder();
            sb.append("Based on your transaction records");
            if (thisMonthSpecific) {
                sb.append(" for this month (").append(now.format(DateTimeFormatter.ofPattern("MMMM yyyy"))).append(")");
            }
            sb.append(":\n\n");

            sb.append("1. **Total Food Spending**: **₹").append(formatAmount(totalFood))
                    .append("** across ").append(matchedTxns.size()).append(" transaction(s).\n");

            if (!matchedTxns.isEmpty()) {
                sb.append("2. **Recent Activity**:\n");
                matchedTxns.stream().limit(3).forEach(t -> {
                    String dateStr = t.getTxnDate() != null ? t.getTxnDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "";
                    String bank = (t.getAccount() != null && t.getAccount().getBankName() != null) ? t.getAccount().getBankName() : "Account";
                    sb.append("   • ₹").append(formatAmount(t.getAmount()))
                            .append(" (").append(t.getCategory()).append(") on ")
                            .append(dateStr).append(" via ").append(bank).append("\n");
                });
            }

            if (foodBudget.isPresent()) {
                Budget b = foodBudget.get();
                BigDecimal pct = b.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0 ?
                        b.getSpentAmount().multiply(new BigDecimal("100")).divide(b.getBudgetAmount(), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                sb.append("3. **Budget Status**: You budgeted ₹").append(formatAmount(b.getBudgetAmount()))
                        .append(" for Food (").append(pct).append("% utilized).\n");
            } else {
                sb.append("3. **Budget Status**: No dedicated Food budget has been set yet. You can create one in the Budgets section.\n");
            }

            sb.append("4. **Tip**: Cooking at home and meal-planning can trim dining expenses by 25–40% each month.");
            return sb.toString();
        }

        // 2. Other categories: Shopping, Travel, Utilities, Entertainment
        for (String catKey : List.of("shopping", "travel", "transport", "utilities", "bills", "entertainment", "health", "investment")) {
            if (q.contains(catKey)) {
                List<Transaction> catTxns = allTransactions.stream()
                        .filter(t -> t.getType() == Transaction.TransactionType.DEBIT)
                        .filter(t -> (t.getCategory() != null && t.getCategory().toLowerCase().contains(catKey)) ||
                                     (t.getMerchant() != null && t.getMerchant().toLowerCase().contains(catKey)))
                        .collect(Collectors.toList());

                BigDecimal totalCat = catTxns.stream()
                        .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                return "=== " + catKey.toUpperCase() + " EXPENSES ===\n" +
                        "1. Total recorded " + catKey + " spending: ₹" + formatAmount(totalCat) +
                        " across " + catTxns.size() + " transaction(s).\n" +
                        "2. Ensure you have an active budget limit set for " + catKey + " to keep expenditures controlled.";
            }
        }

        // 3. Total spend / expenses
        if (q.contains("how much did i spend") || q.contains("total spend") || q.contains("total expense") ||
            q.contains("spending this month") || q.contains("expenses this month")) {
            BigDecimal monthSpend = currentMonthTxns.stream()
                    .filter(t -> t.getType() == Transaction.TransactionType.DEBIT)
                    .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, BigDecimal> catSpend = getCategorySpending(currentMonthTxns);
            StringBuilder sb = new StringBuilder();
            sb.append("Your total spending for this month is **₹").append(formatAmount(monthSpend)).append("**.\n\n");
            sb.append("Category Breakdown:\n");
            catSpend.forEach((cat, amt) -> sb.append("• **").append(cat).append("**: ₹").append(formatAmount(amt)).append("\n"));
            return sb.toString();
        }

        // 4. Savings Goals
        if (q.contains("goal") || q.contains("track") || q.contains("target") || q.contains("savings")) {
            return generateSmartGoalAdvice(userId);
        }

        // 5. Cut back / Over budget
        if (q.contains("cut back") || q.contains("reduce") || q.contains("over budget") || q.contains("which category")) {
            Map<String, BigDecimal> catSpend = getCategorySpending(currentMonthTxns);
            String topCat = catSpend.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("General");
            BigDecimal topAmt = catSpend.getOrDefault(topCat, BigDecimal.ZERO);

            return "=== CATEGORY OPTIMIZATION ===\n" +
                    "1. **Highest Spending Category**: You spent the most on **" + topCat + "** (₹" + formatAmount(topAmt) + ").\n" +
                    "2. **Primary Recommendation**: Focus on trimming non-essential purchases in " + topCat + " first.\n" +
                    "3. **Budgeting Action**: Set a strict monthly spending cap under the Budgets tab to receive automatic warning alerts when reaching 80% and 100%.";
        }

        // 6. Bank Balance / Accounts
        if (q.contains("balance") || q.contains("how much money") || q.contains("account balance") || q.contains("bank")) {
            BigDecimal totalBalance = accounts.stream()
                    .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StringBuilder sb = new StringBuilder();
            sb.append("=== 💳 ACCOUNT BALANCES ===\n");
            sb.append("Total Consolidated Balance: **₹").append(formatAmount(totalBalance)).append("** across ")
                    .append(accounts.size()).append(" account(s):\n\n");
            accounts.forEach(a -> sb.append("• **").append(a.getBankName())
                    .append("** (").append(a.getAccountType()).append(a.getMaskedNumber() != null ? " - " + a.getMaskedNumber() : "").append("): ₹")
                    .append(formatAmount(a.getBalance())).append("\n"));
            return sb.toString();
        }

        // 7. How to save more money
        if (q.contains("save more") || q.contains("how can i save") || q.contains("saving tips")) {
            return "=== 💡 ACTIONABLE SAVINGS STRATEGIES ===\n" +
                    "1. **Follow the 50-30-20 Rule**: Dedicate 50% to needs, 30% to wants, and save/invest at least 20% right upon receiving income.\n" +
                    "2. **Review High-Frequency Expenses**: Check your Food and Shopping expenses to eliminate unused recurring subscriptions and dine-outs.\n" +
                    "3. **Automate Recurring Goals**: Set a recurring transfer to your savings account on payday.\n" +
                    "4. **Build an Emergency Cushion**: Keep 3–6 months of mandatory living expenses easily accessible in liquid funds.";
        }

        // 8. General Financial Intelligence Fallback
        BigDecimal totalBal = accounts.stream().map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebit = currentMonthTxns.stream().filter(t -> t.getType() == Transaction.TransactionType.DEBIT).map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        return "Based on your current financial records:\n" +
                "1. **Available Liquidity**: ₹" + formatAmount(totalBal) + " across " + accounts.size() + " account(s).\n" +
                "2. **Monthly Expenses**: ₹" + formatAmount(totalDebit) + " recorded this month.\n" +
                "3. **Active Budgets & Goals**: You have " + budgets.size() + " budget(s) and " + goals.size() + " goal(s) registered.\n\n" +
                "Regarding your question ('" + question + "'): For specific insights, try asking about your spending on Food, your bank balances, or your savings goals!";
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private boolean isFoodTransaction(Transaction t) {
        String cat = t.getCategory() != null ? t.getCategory().toLowerCase() : "";
        String merch = t.getMerchant() != null ? t.getMerchant().toLowerCase() : "";
        String desc = t.getDescription() != null ? t.getDescription().toLowerCase() : "";

        return cat.contains("food") || cat.contains("dining") || cat.contains("groceries") || cat.contains("grocery") ||
                merch.contains("food") || merch.contains("swiggy") || merch.contains("zomato") || merch.contains("restaurant") ||
                desc.contains("food") || desc.contains("dining") || desc.contains("grocery");
    }

    private boolean isSameMonth(LocalDateTime date, LocalDateTime target) {
        if (date == null || target == null) return false;
        return date.getMonth() == target.getMonth() && date.getYear() == target.getYear();
    }

    private List<Transaction> filterCurrentMonthTransactions(List<Transaction> transactions, LocalDateTime now) {
        List<Transaction> filtered = transactions.stream()
                .filter(t -> isSameMonth(t.getTxnDate(), now))
                .collect(Collectors.toList());
        // If current month has 0 transactions, fallback to all transactions so demo data is always analyzed
        return filtered.isEmpty() ? transactions : filtered;
    }

    private Map<String, BigDecimal> getCategorySpending(List<Transaction> transactions) {
        Map<String, BigDecimal> map = new HashMap<>();
        transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.DEBIT)
                .forEach(t -> {
                    String cat = (t.getCategory() != null && !t.getCategory().isBlank()) ? t.getCategory() : "Other";
                    BigDecimal amt = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
                    map.merge(cat, amt, BigDecimal::add);
                });
        return map;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
}

