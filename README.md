# Personal Finance and Budget Management Application

A secure, enterprise-grade personal finance and budget management web application engineered according to the SRS specification. Built with **Spring Boot 3.2.0 (Java 17)** and **React.js (React Router v6)** with **MySQL 8.0** persistence.

---

## 🚀 Live Deployment Links

| Environment | URL | Details |
| :--- | :--- | :--- |
| **Live Full-Stack App (Public Cloud)** | [https://5a07da4d8df462.lhr.life](https://5a07da4d8df462.lhr.life) | Complete unified application (Frontend SPA + Spring Boot REST API + Live Database) |
| **GitHub Pages** | [https://keerthana-rajamani.github.io/finanace/](https://keerthana-rajamani.github.io/finanace/) | Production React client build hosted via GitHub Pages |
| **GitHub Repository** | [https://github.com/keerthana-rajamani/finanace](https://github.com/keerthana-rajamani/finanace) | Source code, container configs, and deployment manifests |
| **Local Unified App** | `http://localhost:8080` | Spring Boot serving bundled React SPA + REST APIs |
| **Local React Dev Server** | `http://localhost:8081` | Standalone React development server with hot-reloading |

---

## 🌟 Core Features & SRS Compliance

1. **Authentication & Security (FR1, FR2)**:
   - Stateless JWT authentication with HMAC-SHA256 (`/api/auth/register`, `/api/auth/login`).
   - BCrypt password encryption (8+ characters, complex validation).
   - Role-Based Access Control (`USER`, `ADMIN`).
   - Standard HTTP status codes (200, 201, 400, 401, 403, 404, 500).

2. **Bank Account Management & Sync (FR3 - FR5, Appendix H)**:
   - Account linking with masking: `POST /api/accounts/link`.
   - Automated transaction sync: `POST /api/transactions/sync`.
   - Multi-account balance aggregation.

3. **Transaction Management (FR6 - FR9)**:
   - Filter transactions by date range, category, type (`INCOME`, `EXPENSE`), and account.
   - Live CSV, Excel, and PDF transaction exports (`/api/transactions/export/pdf`, `/api/transactions/export/excel`).

4. **Budget Management (FR10 - FR13)**:
   - Category-wise monthly budget allocation (`POST /api/budgets`).
   - Real-time spending progress bars and warning notifications (80% and 100% threshold alerts).
   - Strict validation preventing non-positive amounts with `BudgetValidationException`.

5. **Financial Goals & Savings (FR14 - FR17)**:
   - Target tracking with deadline alerts (`POST /api/goals`).
   - Strict validation: target amount must strictly exceed current savings (`<=` check).

6. **Investments & Net Worth (SRS Section 2 & Appendix H)**:
   - Portfolio tracking (`GET /api/investments`) with NAV, units, invested amount, current value, and annualized XIRR.
   - Dynamic asset allocation breakdown (Equity, Debt, Gold, Cash).
   - Real-time Net Worth calculation (`GET /api/networth`) aggregating Total Assets minus Total Liabilities.

---

## 🛠️ Architecture & Tech Stack

```
   ┌────────────────────────────────────────────────────────┐
   │             React 18 Single Page Application           │
   │        React Router v6  •  Bootstrap 5  •  Axios       │
   └───────────────────────────┬────────────────────────────┘
                               │ JSON / REST (JWT Bearer)
                               ▼
   ┌────────────────────────────────────────────────────────┐
   │             Spring Boot 3.2.0 (Java 17)                │
   │  Spring Security  •  Spring Data JPA  •  Validation    │
   └───────────────────────────┬────────────────────────────┘
                               │ JDBC / Hibernate
                               ▼
   ┌────────────────────────────────────────────────────────┐
   │            MySQL 8.0 / H2 Relational Database          │
   └────────────────────────────────────────────────────────┘
```

- **Backend**: Spring Boot 3.2.0, Java 17, Spring Security 6, JJWT 0.11.5, Spring Data JPA, iTextPDF, Apache POI.
- **Frontend**: React 18, React Router v6.28.0, Bootstrap 5, FontAwesome icons.
- **Database**: MySQL 8.0 (`financedb`) on port 3306; H2 in-memory mode for automated unit tests.
- **DevOps**: Docker, Docker Compose, Kubernetes manifests, AWS ECS Task Definitions, GitHub Actions CI/CD.

---

## 📦 How to Run Locally

### Option 1: 1-Click Windows Scripts
- **Start All**: Double-click `deploy.bat` (builds React, compiles Spring Boot, and launches both services).
- **Stop All**: Double-click `stop.bat` (terminates port 8080 and 8081 listeners cleanly).

### Option 2: Manual Start
```bash
# 1. Start Backend (Port 8080)
cd springapp
mvn clean spring-boot:run

# 2. Start Frontend (Port 8081)
cd reactapp
npm install
npm start
```

### Option 3: Docker Compose
```bash
docker-compose up --build -d
```

---

## 🧪 Test Suite Execution

- **Spring Boot Backend**: 9 automated integration tests covering security, exceptions, validations, and business logic:
  ```bash
  cd springapp
  mvn test
  ```
- **React Frontend**: Automated component and route verification:
  ```bash
  cd reactapp
  npm test -- --watchAll=false
  ```