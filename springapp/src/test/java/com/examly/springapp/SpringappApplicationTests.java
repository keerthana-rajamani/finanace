package com.examly.springapp;

import com.examly.springapp.dto.AuthDto.*;
import com.examly.springapp.exception.*;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SpringappApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private TransactionService transactionService;

    @Test
    void contextLoads() {
        assertNotNull(authService);
    }

    @Test
    @Transactional
    void testUserRegistrationSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setEmail("testuser@example.com");
        req.setPhone("9876543210");
        req.setPassword("Password@123");
        req.setRole("USER");

        AuthResponse res = authService.register(req);
        assertNotNull(res.getToken());
        assertEquals("testuser@example.com", res.getEmail());
        assertEquals("Test User", res.getName());
    }

    @Test
    void testUserRegistrationInvalidName() {
        RegisterRequest req = new RegisterRequest();
        req.setName("User123");
        req.setEmail("user123@example.com");
        req.setPhone("9876543210");
        req.setPassword("Password@123");

        assertThrows(InvalidNameException.class, () -> authService.register(req));
    }

    @Test
    void testUserRegistrationInvalidPhone() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Valid Name");
        req.setEmail("validphone@example.com");
        req.setPhone("12345");
        req.setPassword("Password@123");

        assertThrows(InvalidPhoneException.class, () -> authService.register(req));
    }

    @Test
    void testUserRegistrationPasswordTooShort() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Valid Name");
        req.setEmail("shortpw@example.com");
        req.setPhone("9876543210");
        req.setPassword("short");

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    @Transactional
    void testBudgetValidationNegativeAmount() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Budget User");
        req.setEmail("budgetUser@example.com");
        req.setPhone("9123456780");
        req.setPassword("Password@123");
        authService.register(req);

        User user = userRepository.findByEmail("budgetUser@example.com").orElseThrow();

        Budget invalidBudget = new Budget();
        invalidBudget.setCategory("Food");
        invalidBudget.setBudgetAmount(BigDecimal.valueOf(-100));

        assertThrows(BudgetValidationException.class, () -> budgetService.saveBudget(user.getId(), invalidBudget));
    }

    @Test
    @Transactional
    void testBudgetCreationAndAutoUpdate() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Budget User Two");
        req.setEmail("budgetUser2@example.com");
        req.setPhone("9123456781");
        req.setPassword("Password@123");
        authService.register(req);

        User user = userRepository.findByEmail("budgetUser2@example.com").orElseThrow();

        Budget budget = new Budget();
        budget.setCategory("Food");
        budget.setBudgetAmount(BigDecimal.valueOf(5000));
        budget.setMonth(LocalDate.now().withDayOfMonth(1));

        Budget saved = budgetService.saveBudget(user.getId(), budget);
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(saved.getBudgetAmount()));

        // Updating same category and month should update existing
        Budget budgetUpdate = new Budget();
        budgetUpdate.setCategory("Food");
        budgetUpdate.setBudgetAmount(BigDecimal.valueOf(6000));
        budgetUpdate.setMonth(LocalDate.now().withDayOfMonth(1));

        Budget updated = budgetService.saveBudget(user.getId(), budgetUpdate);
        assertEquals(saved.getId(), updated.getId());
        assertEquals(0, BigDecimal.valueOf(6000).compareTo(updated.getBudgetAmount()));
    }

    @Test
    @Transactional
    void testGoalValidationTargetLessThanOrEqualCurrent() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Goal User");
        req.setEmail("goalUser@example.com");
        req.setPhone("9123456782");
        req.setPassword("Password@123");
        authService.register(req);

        User user = userRepository.findByEmail("goalUser@example.com").orElseThrow();

        Goal goal = new Goal();
        goal.setName("Vacation");
        goal.setTargetAmount(BigDecimal.valueOf(1000));
        goal.setCurrentAmount(BigDecimal.valueOf(1000));
        goal.setTargetDate(LocalDate.now().plusMonths(6));

        assertThrows(IllegalArgumentException.class, () -> goalService.createGoal(user.getId(), goal));
    }

    @Test
    @Transactional
    void testTransactionUpdatesBalanceAndBudget() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Txn User");
        req.setEmail("txnUser@example.com");
        req.setPhone("9123456783");
        req.setPassword("Password@123");
        authService.register(req);

        User user = userRepository.findByEmail("txnUser@example.com").orElseThrow();

        Account account = new Account();
        account.setBankName("HDFC Bank");
        account.setAccountType(Account.AccountType.SAVINGS);
        account.setBalance(BigDecimal.valueOf(10000));
        Account savedAccount = accountService.addAccount(user.getId(), account);

        Budget budget = new Budget();
        budget.setCategory("Dining");
        budget.setBudgetAmount(BigDecimal.valueOf(3000));
        budgetService.saveBudget(user.getId(), budget);

        Transaction txn = new Transaction();
        txn.setType(Transaction.TransactionType.DEBIT);
        txn.setAmount(BigDecimal.valueOf(500));
        txn.setCategory("Dining");
        txn.setMerchant("Restaurant");
        transactionService.addTransaction(savedAccount.getId(), txn);

        Account reloaded = accountRepository.findById(savedAccount.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(9500).compareTo(reloaded.getBalance()));

        Budget updatedBudget = budgetService.getCurrentMonthBudgets(user.getId()).stream()
                .filter(b -> "Dining".equals(b.getCategory()))
                .findFirst().orElseThrow();
        assertEquals(0, BigDecimal.valueOf(500).compareTo(updatedBudget.getSpentAmount()));
    }
}
