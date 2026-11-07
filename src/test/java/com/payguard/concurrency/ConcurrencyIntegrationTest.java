package com.payguard.concurrency;

import com.payguard.dto.TransactionRequest;
import com.payguard.dto.TransactionResponse;
import com.payguard.entity.TransactionStatus;
import com.payguard.entity.TransactionType;
import com.payguard.entity.Wallet;
import com.payguard.exception.DuplicateTransactionException;
import com.payguard.exception.InsufficientBalanceException;
import com.payguard.repository.TransactionRepository;
import com.payguard.repository.WalletRepository;
import com.payguard.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConcurrencyIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrency: 10 simultaneous debit requests of ₹100 against ₹500 balance must yield exactly 5 successes, 5 failures, and ₹0 final balance")
    void testConcurrentDebitRequestsPreventNegativeBalance() throws Exception {
        String userId = "USER_CONC_DEBIT_10";
        walletRepository.saveAndFlush(new Wallet(userId, new BigDecimal("500.00")));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 1; i <= threadCount; i++) {
            final String txnId = "TXN_CONC_DEBIT_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionRequest request = new TransactionRequest(
                            txnId,
                            userId,
                            new BigDecimal("100.00"),
                            TransactionType.DEBIT
                    );
                    TransactionResponse response = transactionService.process(request);
                    if (response.getStatus() == TransactionStatus.SUCCESS) {
                        successCount.incrementAndGet();
                    }
                } catch (InsufficientBalanceException ex) {
                    failureCount.incrementAndGet();
                } catch (Throwable ex) {
                    // unexpected error
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire all 10 threads simultaneously
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(completed, "All 10 threads must complete within 10 seconds");
        assertEquals(5, successCount.get(), "Exactly 5 debit transactions must succeed");
        assertEquals(5, failureCount.get(), "Exactly 5 debit transactions must fail with InsufficientBalanceException");

        Wallet updatedWallet = walletRepository.findByUserId(userId).orElseThrow();
        assertEquals(new BigDecimal("0.0000"), updatedWallet.getBalance(), "Final wallet balance must be strictly ₹0.0000, never negative");
    }

    @Test
    @DisplayName("Concurrency: 10 simultaneous duplicate requests with identical transactionId must mutate balance exactly ONCE")
    void testConcurrentDuplicateRequestsMutateBalanceOnce() throws Exception {
        String userId = "USER_CONC_DUP_10";
        walletRepository.saveAndFlush(new Wallet(userId, BigDecimal.ZERO));

        int threadCount = 10;
        String sharedTxnId = "TXN_CONC_SHARED_001";
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger firstSuccess = new AtomicInteger();
        AtomicInteger duplicateHandled = new AtomicInteger();

        for (int i = 1; i <= threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionRequest request = new TransactionRequest(
                            sharedTxnId,
                            userId,
                            new BigDecimal("100.00"),
                            TransactionType.CREDIT
                    );
                    TransactionResponse response = transactionService.process(request);
                    if (response.getMessage().contains("Duplicate transaction detected")) {
                        duplicateHandled.incrementAndGet();
                    } else if (response.getStatus() == TransactionStatus.SUCCESS) {
                        firstSuccess.incrementAndGet();
                    }
                } catch (DuplicateTransactionException ex) {
                    duplicateHandled.incrementAndGet();
                } catch (Throwable ex) {
                    // unexpected error
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire all 10 duplicate requests simultaneously
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(completed, "All 10 duplicate requests must complete within 10 seconds");
        assertEquals(1, firstSuccess.get(), "Only 1 thread must perform the initial balance mutation");
        assertEquals(9, duplicateHandled.get(), "The other 9 threads must be recognized and handled as duplicates");

        Wallet updatedWallet = walletRepository.findByUserId(userId).orElseThrow();
        assertEquals(new BigDecimal("100.0000"), updatedWallet.getBalance(), "Wallet balance must be credited exactly once (₹100.0000)");
    }
}
