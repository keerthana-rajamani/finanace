# Backend Documentation — Personal Finance & Budget Management Application

## Tech Stack
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** (JWT-based stateless auth)
- **Spring Data JPA** (Hibernate ORM)
- **MySQL 8.0** (database)
- **Lombok** (boilerplate reduction)
- **JJWT 0.11.5** (JWT token generation & validation)
- **Spring WebFlux** (WebClient for Gemini AI API calls)
- **Jakarta Validation** (bean validation)

---

## Project Structure

```
springapp/
└── src/main/java/com/examly/springapp/
    ├── controller/       — REST API endpoints
    ├── service/          — Business logic
    ├── repository/       — Database access (Spring Data JPA)
    ├── model/            — JPA entity classes
    ├── dto/              — Data Transfer Objects
    ├── security/         — JWT filter, utility, security config
    ├── exception/        — Custom exceptions & global handler
    └── SpringappApplication.java
```

---

## Layer-by-Layer Breakdown

---

### 1. `model/` — Entity Classes

JPA entities that map directly to MySQL tables. Each entity uses Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` to auto-generate getters, setters, and constructors.

| Class | Table | Description |
|---|---|---|
| `User` | `users` | Registered user with name, email, phone, password, role |
| `Account` | `accounts` | Linked bank account (savings, current, credit, DEMAT) |
| `Transaction` | `transactions` | Debit/credit transaction linked to an account |
| `Budget` | `budgets` | Monthly category-wise budget with spend tracking |
| `Goal` | `goals` | Financial goal with target amount, date, priority |
| `Bill` | `bills` | Recurring bill with due date and payment status |
| `TaxSummary` | `tax_summaries` | Annual tax computation with income, deductions, estimated tax |

**Sample — `User.java`**
```java
@Entity
@Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "Name must not contain numbers or special characters")
    private String name;

    @Email
    @Column(unique = true)
    private String email;

    @Pattern(regexp = "^\\d{10}$", message = "Phone Number must be exactly 10 digits long")
    private String phone;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    public enum Role { USER, FAMILY_MEMBER, FINANCIAL_ADVISOR, SUPPORT_AGENT, ADMIN }
}
```

**Sample — `Transaction.java`**
```java
@Entity
@Table(name = "transactions")
@Data @NoArgsConstructor @AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user", "transactions"})
    private Account account;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;   // DEBIT or CREDIT

    private String category;
    private String merchant;
    private String description;
    private LocalDateTime txnDate;

    public enum TransactionType { DEBIT, CREDIT }
}
```

---

### 2. `repository/` — Data Access Layer

Interfaces extending `JpaRepository<Entity, Long>`. Spring Data JPA auto-generates SQL from method names — no manual queries needed.

| Repository | Key Methods |
|---|---|
| `UserRepository` | `findByEmail`, `existsByEmail` |
| `AccountRepository` | `findByUserId` |
| `TransactionRepository` | `findByAccountUserIdOrderByTxnDateDesc`, `findByAccountIdOrderByTxnDateDesc` |
| `BudgetRepository` | `findByUserId`, `findByUserIdAndMonth`, `findByUserIdAndCategoryAndMonth` |
| `GoalRepository` | `findByUserId` |
| `BillRepository` | `findByUserId`, `findByUserIdAndNextDueDateBetween`, `findByUserIdAndStatus` |
| `TaxSummaryRepository` | `findByUserId`, `findByUserIdAndFinancialYear` |

**Sample — `BudgetRepository.java`**
```java
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    List<Budget> findByUserIdAndMonth(Long userId, LocalDate month);
    Optional<Budget> findByUserIdAndCategoryAndMonth(Long userId, String category, LocalDate month);
}
```

---

### 3. `service/` — Business Logic Layer

Contains all business rules, validations, and orchestration between repositories. Controllers call services; services call repositories.

| Service | Responsibility |
|---|---|
| `AuthService` | Register & login — validates name/phone, encodes password, generates JWT |
| `AccountService` | Link/unlink bank accounts per user |
| `TransactionService` | Add/delete transactions; auto-syncs budget `spentAmount` on DEBIT |
| `BudgetService` | Create/update/delete monthly budgets; returns current month summary |
| `GoalService` | CRUD for financial goals with target vs current amount validation |
| `BillService` | CRUD for bills; calculates `nextDueDate`; marks bills as paid |
| `TaxService` | Computes taxable income & estimated tax (new regime slabs); auto-creates summary from transactions |
| `GeminiService` | Builds financial context from DB and calls Google Gemini AI API |
| `UserDetailsServiceImpl` | Loads user by email for Spring Security authentication |

**Sample — `TransactionService.java`**
```java
@Service
public class TransactionService {

    @Transactional
    public Transaction addTransaction(Long accountId, Transaction transaction) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        transaction.setAccount(account);
        if (transaction.getTxnDate() == null) transaction.setTxnDate(LocalDateTime.now());
        Transaction saved = transactionRepository.save(transaction);

        // Auto-sync budget spentAmount for DEBIT transactions
        if (saved.getType() == Transaction.TransactionType.DEBIT && saved.getCategory() != null) {
            syncBudget(account.getUser().getId(), saved.getCategory(), saved.getAmount(), true);
        }
        return saved;
    }

    private void syncBudget(Long userId, String category, BigDecimal amount, boolean add) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        budgetRepository.findByUserIdAndCategoryAndMonth(userId, category, firstOfMonth)
                .ifPresent(budget -> {
                    BigDecimal current = budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO;
                    budget.setSpentAmount(add ? current.add(amount) : current.subtract(amount).max(BigDecimal.ZERO));
                    budgetRepository.save(budget);
                });
    }
}
```

**Sample — `BillService.java` (nextDueDate calculation)**
```java
public Bill createBill(Long userId, Bill bill) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    bill.setUser(user);
    bill.setNextDueDate(calculateNextDueDate(bill.getDueDayOfMonth()));
    bill.setStatus(Bill.Status.PENDING);
    return billRepository.save(bill);
}

private LocalDate calculateNextDueDate(int dueDayOfMonth) {
    LocalDate today = LocalDate.now();
    LocalDate dueThisMonth = today.withDayOfMonth(Math.min(dueDayOfMonth, today.lengthOfMonth()));
    return dueThisMonth.isBefore(today) ? dueThisMonth.plusMonths(1) : dueThisMonth;
}
```

---

### 4. `controller/` — REST API Layer

All controllers are annotated with `@RestController`. Every protected endpoint uses `AuthHelper` to extract the current user's ID from the JWT security context.

| Controller | Base URL | Endpoints |
|---|---|---|
| `AuthController` | `/api/auth` | POST `/register`, POST `/login`, POST `/logout` |
| `AccountController` | `/api/accounts` | POST, GET, DELETE `/{id}` |
| `TransactionController` | `/api/transactions` | POST `/account/{accountId}`, GET, GET `/account/{accountId}`, DELETE `/{id}` |
| `BudgetController` | `/api/budgets` | POST, GET, GET `/summary`, PUT `/{id}`, DELETE `/{id}` |
| `GoalController` | `/api/goals` | POST, GET, PUT `/{id}`, DELETE `/{id}` |
| `BillController` | `/api/bills` | POST, GET, GET `/upcoming`, PUT `/{id}`, PUT `/{id}/pay`, DELETE `/{id}` |
| `TaxController` | `/api/tax` | GET `/summary?year=`, GET `/all`, POST, PUT `/{id}` |
| `AIInsightsController` | `/api/ai` | GET `/insights`, GET `/spending-analysis`, GET `/budget-recommendations`, GET `/goal-advice`, POST `/ask` |
| `UserController` | `/api/users` | GET `/profile` |

**Sample — `GoalController.java`**
```java
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @PostMapping
    public ResponseEntity<Goal> createGoal(@Valid @RequestBody Goal goal) {
        return ResponseEntity.ok(goalService.createGoal(authHelper.getCurrentUserId(), goal));
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
```

---

### 5. `dto/` — Data Transfer Objects

Used to decouple the API request/response shape from the entity model. Prevents exposing sensitive fields like `password` in responses.

**`AuthDto.java`**
```java
public class AuthDto {

    @Data
    public static class RegisterRequest {
        private String name;
        private String email;
        private String phone;
        private String password;
        private String role;
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String email;
        private String role;
        private String name;
    }
}
```

---

### 6. `security/` — JWT Security

| Class | Role |
|---|---|
| `JwtUtil` | Generates and validates JWT tokens; extracts email and role from claims |
| `JwtFilter` | `OncePerRequestFilter` — intercepts every request, validates Bearer token, sets `SecurityContext` |
| `SecurityConfig` | Disables CSRF, configures CORS, sets stateless session, permits `/api/auth/**` publicly |

**Sample — `JwtUtil.java`**
```java
public String generateToken(String email, String role) {
    return Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
}

public boolean validateToken(String token, UserDetails userDetails) {
    String email = extractEmail(token);
    return email.equals(userDetails.getUsername()) && !isExpired(token);
}
```

**Sample — `SecurityConfig.java` (CORS + JWT filter)**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

---

### 7. `exception/` — Error Handling

All exceptions are caught centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`) and returned as structured JSON `{ "error": "message" }`.

| Exception | HTTP Status | Trigger |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entity not found in DB |
| `UnauthorisedAccessException` | 403 | User tries to access another user's data |
| `DuplicateTransactionException` | 409 | Email already registered |
| `InvalidNameException` | 400 | Name contains numbers or special characters |
| `InvalidPhoneException` | 400 | Phone is not exactly 10 digits |
| `IllegalArgumentException` | 400 | Business rule violation (e.g. target < current in goals) |
| `MethodArgumentNotValidException` | 400 | `@Valid` bean validation failure |
| `Exception` (catch-all) | 500 | Unexpected server error |

**Sample — `GlobalExceptionHandler.java`**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
```

---

### 8. `AuthHelper` — Current User Utility

A Spring `@Component` used by all controllers to get the authenticated user's ID or full `User` object from the JWT security context without repeating boilerplate.

```java
@Component
public class AuthHelper {

    public Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
```

---

## API Quick Reference

### Authentication (Public)
```
POST /api/auth/register    — Register new user
POST /api/auth/login       — Login, returns JWT token
POST /api/auth/logout      — Logout (client clears token)
```

### All other endpoints require: `Authorization: Bearer <token>`

```
# Accounts
GET    /api/accounts               — Get all linked accounts
POST   /api/accounts               — Link new account
DELETE /api/accounts/{id}          — Unlink account

# Transactions
GET    /api/transactions                        — All transactions for user
POST   /api/transactions/account/{accountId}    — Add transaction to account
DELETE /api/transactions/{id}                   — Delete transaction

# Budgets
GET    /api/budgets                — All budgets
GET    /api/budgets/summary        — Current month budgets with spentAmount
POST   /api/budgets                — Create budget
PUT    /api/budgets/{id}           — Update budget
DELETE /api/budgets/{id}           — Delete budget

# Goals
GET    /api/goals                  — All goals
POST   /api/goals                  — Create goal
PUT    /api/goals/{id}             — Update goal
DELETE /api/goals/{id}             — Delete goal

# Bills
GET    /api/bills                  — All bills
GET    /api/bills/upcoming         — Bills due within 7 days
POST   /api/bills                  — Add bill
PUT    /api/bills/{id}             — Update bill
PUT    /api/bills/{id}/pay         — Mark bill as paid
DELETE /api/bills/{id}             — Delete bill

# Tax
GET    /api/tax/summary?year=2024  — Get/auto-compute tax summary for FY
GET    /api/tax/all                — All tax summaries
POST   /api/tax                    — Save tax summary
PUT    /api/tax/{id}               — Update tax summary

# AI Insights (Gemini)
GET    /api/ai/insights                — Financial health insights
GET    /api/ai/spending-analysis       — Spending pattern analysis
GET    /api/ai/budget-recommendations  — Budget allocation recommendations
GET    /api/ai/goal-advice             — Goal achievement advice
POST   /api/ai/ask                     — Ask a custom finance question
```

---

## Database Configuration (`application.properties`)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/financedb?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=1234
spring.jpa.hibernate.ddl-auto=update
server.port=8080
jwt.secret=finance_secret_key_256bit_minimum_length_required_here
jwt.expiration=28800000
gemini.api.key=<YOUR_GEMINI_API_KEY>
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
```

---

## Request & Response Examples

### Register
```json
POST /api/auth/register
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "9876543210",
  "password": "password123",
  "role": "USER"
}

Response 200:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "role": "USER",
  "name": "John Doe"
}
```

### Add Transaction
```json
POST /api/transactions/account/1
Authorization: Bearer <token>
{
  "amount": 1500.00,
  "type": "DEBIT",
  "category": "Food",
  "merchant": "Swiggy",
  "description": "Dinner order"
}

Response 200:
{
  "id": 5,
  "amount": 1500.00,
  "type": "DEBIT",
  "category": "Food",
  "merchant": "Swiggy",
  "txnDate": "2024-11-15T20:30:00"
}
```

### Create Goal
```json
POST /api/goals
Authorization: Bearer <token>
{
  "name": "Emergency Fund",
  "targetAmount": 100000,
  "currentAmount": 25000,
  "targetDate": "2025-12-31",
  "priority": "HIGH"
}

Response 200:
{
  "id": 3,
  "name": "Emergency Fund",
  "targetAmount": 100000.00,
  "currentAmount": 25000.00,
  "targetDate": "2025-12-31",
  "priority": "HIGH",
  "status": "ACTIVE"
}
```

### Error Response
```json
Response 400:
{
  "error": "Target amount must exceed current savings"
}

Response 404:
{
  "error": "Goal not found"
}

Response 403:
{
  "error": "Access denied"
}
```
