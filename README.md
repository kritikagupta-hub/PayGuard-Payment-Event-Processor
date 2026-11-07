# PayGuard – Payment Event Processor

PayGuard is a robust, production-grade payment event processor built with **Java 17**, **Spring Boot**, **Spring Data JPA**, and **H2 Database**. It guarantees transactional integrity, idempotency, and concurrency safety for mission-critical financial transactions.

---

## 🚀 Key Features

- **Wallet Management**: Create wallets, generate unique IDs, manage balances with `BigDecimal` precision, and check balances.
- **Payment Processing**: Process `CREDIT` and `DEBIT` transactions with immutable transaction history.
- **Idempotency Engine**: Deduplicate incoming payment events using database constraints and idempotency checks to ensure money is moved exactly once.
- **Concurrency & Race Condition Handling**: Pessimistic database locking (`PESSIMISTIC_WRITE`) and transaction isolation to prevent lost updates, race conditions, and negative balances under high concurrent load.
- **Validation & Exception Handling**: Robust DTO validation with uniform, descriptive API error responses.

---

## 🛠️ Technology Stack

- **Language**: Java 17 LTS
- **Framework**: Spring Boot 3.3.4
- **Modules**: Spring Web, Spring Data JPA, Spring Validation
- **Database**: H2 In-Memory Database (with H2 Web Console)
- **Build Tool**: Apache Maven
- **Testing**: JUnit 5, Spring Boot Test

---

## 📁 Layered Architecture

```text
src/main/java/com/payguard/
├── controller/     # REST Controllers
├── service/        # Business Logic & Transaction boundaries
├── repository/     # Spring Data JPA Repositories
├── entity/         # JPA Entities with DB constraints
├── dto/            # Data Transfer Objects & Validation
├── exception/      # Custom Exceptions & Global Exception Handler
├── config/         # Application Configurations
└── PayGuardApplication.java
```

---

## 🚦 Roadmap & Implementation Progress

- [x] **Section 1**: Project Initialization (Spring Boot setup, Maven, H2, package structure, git repository)
- [x] **Section 2**: Database and Entities (Wallet & TransactionRecord)
- [x] **Section 3**: Repository Layer (WalletRepository & TransactionRepository)
- [x] **Section 4**: Wallet Management APIs (Create, Get, Balance check)
- [ ] **Section 5**: Transaction Processing (Credit, Debit, Balance update, Audit history)
- [ ] **Section 6**: Idempotency (Duplicate detection, Replay handling)
- [ ] **Section 7**: Concurrency Handling (Pessimistic locking, Concurrent debits)
- [ ] **Section 8**: Validation and Exception Handling (Custom exceptions, Global handler)
- [ ] **Section 9**: Integration & Concurrency Testing (JUnit 5, 10-thread debit test)
- [ ] **Section 10**: Final Documentation & API Reference

---

## ⚙️ Running Locally

### Prerequisites
- Java 17 or higher
- Maven 3.9+ (or use included Maven wrapper)

### Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

The application will start on port `8080`.
- H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:payguarddb`
  - Username: `sa`
  - Password: *(empty)*

