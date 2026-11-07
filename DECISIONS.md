# Architectural Decision Log (ADR) – PayGuard

## 1. Concurrency Handling & Deadlock Elimination

### Problem
Simultaneous debit requests against the same user's wallet introduce race conditions:
1. **Lost Updates & Negative Balance**: Two concurrent threads read the same balance (e.g., ₹500) and both approve a ₹500 debit, driving the final balance into negative (-₹500).
2. **Inter-Table Deadlocks**: If Thread 1 queries/locks `TRANSACTION_RECORDS` before locking `WALLETS`, while Thread 2 locks `WALLETS` before inserting into `TRANSACTION_RECORDS`, the database engine detects a circular wait deadlock.

### Decision & Solution
- **Pessimistic Locking (`PESSIMISTIC_WRITE`)**: We chose `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) over Optimistic Locking (`@Version`). In financial payment gateways, debit requests on high-velocity wallets face extreme contention; optimistic locking causes massive transaction rollbacks and requires complex client retry logic, whereas pessimistic row locking queues threads neatly at the database row level.
- **Global Lock Ordering**: We established a strict global acquisition sequence:
  ```text
  Wallet lock (findByUserIdWithLock) ──> Query TransactionRecord ──> Insert TransactionRecord
  ```
  Acquiring the `WALLETS` row lock first eliminates lock order inversions and guarantees that no deadlocks occur across multiple concurrent threads.

---

## 2. Two-Tier Idempotency Architecture

### Problem
Network retries, duplicate webhooks, and client double-clicks can submit the same `transactionId` multiple times within milliseconds.

### Decision & Solution
We implemented a two-tier defense:
1. **Layer 1 (Fast-Path Memory/Query Check)**:
   Once the wallet row lock is held, `transactionRepository.findByTransactionId(transactionId)` checks if the transaction was already processed. If found, the original cached `TransactionResponse` is returned immediately without mutating the wallet ledger.
2. **Layer 2 (Database Level Unique Constraint)**:
   The database schema enforces a `UNIQUE` index constraint on `transaction_records(transaction_id)`.
3. **Flushed Inserts (`saveAndFlush`)**:
   Standard JPA `save()` queues SQL statements in the Hibernate persistence context until transaction commit. If two duplicate transactions execute in parallel, deferred flushing causes the constraint violation to trigger outside the service try/catch block, resulting in a generic 500 error. By invoking `saveAndFlush()`, the `INSERT` SQL statement is immediately executed, reliably catching constraint violations and returning clean HTTP responses.

---

## 3. Strict Financial Decimal Precision

### Problem
Using `float` or `double` introduces IEEE 754 binary floating-point rounding errors (e.g. `0.1 + 0.2 = 0.30000000000000004`). In financial ledgers, any rounding drift violates regulatory compliance and accounting audits.

### Decision & Solution
- All balance and transaction amounts strictly use `java.math.BigDecimal`.
- Scale is fixed at `4` decimal places with `RoundingMode.HALF_UP`.
- Database schema stores money as `DECIMAL(19,4)`.
- Input DTOs enforce `@Digits(integer = 15, fraction = 4)` to reject inputs with fractional cents exceeding 4 decimal places before processing begins.
