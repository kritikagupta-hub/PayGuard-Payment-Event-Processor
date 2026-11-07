package com.payguard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payguard.dto.CreateWalletRequest;
import com.payguard.dto.TransactionRequest;
import com.payguard.dto.TransactionResponse;
import com.payguard.entity.TransactionRecord;
import com.payguard.entity.TransactionType;
import com.payguard.entity.Wallet;
import com.payguard.repository.TransactionRepository;
import com.payguard.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentEventProcessorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-End: Full Customer Lifecycle (Create Wallet -> Credit -> Debit -> Balance -> Audit History)")
    void testFullCustomerLifecycle() throws Exception {
        String userId = "USER_LIFECYCLE_01";

        // 1. Create Wallet with initial balance 1000.0000
        CreateWalletRequest createReq = new CreateWalletRequest(userId, new BigDecimal("1000.0000"));
        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(userId)))
                .andExpect(jsonPath("$.balance", is(1000.0000)));

        // 2. Process Credit of 500.2500
        TransactionRequest creditReq = new TransactionRequest(
                "TXN_LIFE_CREDIT_01", userId, new BigDecimal("500.2500"), TransactionType.CREDIT
        );
        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.balanceAfter", is(1500.2500)));

        // 3. Process Debit of 300.1000
        TransactionRequest debitReq = new TransactionRequest(
                "TXN_LIFE_DEBIT_01", userId, new BigDecimal("300.1000"), TransactionType.DEBIT
        );
        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.balanceAfter", is(1200.1500)));

        // 4. Verify Wallet Balance via API
        mockMvc.perform(get("/api/v1/wallets/{userId}/balance", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(1200.1500)));

        // 5. Verify Transaction History is ordered by createdAt descending
        mockMvc.perform(get("/api/v1/transactions/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionId", is("TXN_LIFE_DEBIT_01")))
                .andExpect(jsonPath("$[0].balanceBefore", is(1500.2500)))
                .andExpect(jsonPath("$[0].balanceAfter", is(1200.1500)))
                .andExpect(jsonPath("$[1].transactionId", is("TXN_LIFE_CREDIT_01")))
                .andExpect(jsonPath("$[1].balanceBefore", is(1000.0000)))
                .andExpect(jsonPath("$[1].balanceAfter", is(1500.2500)));
    }

    @Test
    @DisplayName("Concurrency: Mixed Credit and Debit transactions on same wallet maintain exact balance")
    void testConcurrentMixedCreditAndDebitTransactions() throws Exception {
        String userId = "USER_CONC_MIXED";
        walletRepository.saveAndFlush(new Wallet(userId, new BigDecimal("1000.0000")));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // 5 threads credit 100.0000 (+500), 5 threads debit 50.0000 (-250). Expected net balance: 1250.0000
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionType type = (index % 2 == 0) ? TransactionType.CREDIT : TransactionType.DEBIT;
                    BigDecimal amount = (index % 2 == 0) ? new BigDecimal("100.0000") : new BigDecimal("50.0000");

                    TransactionRequest req = new TransactionRequest(
                            "TXN_MIXED_" + index, userId, amount, type
                    );

                    MvcResult result = mockMvc.perform(post("/api/v1/transactions/process")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore or record
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All mixed concurrent transactions should finish within timeout");
        assertEquals(10, successCount.get(), "All 10 valid transactions should succeed");

        Wallet finalWallet = walletRepository.findByUserId(userId).orElseThrow();
        assertEquals(new BigDecimal("1250.0000"), finalWallet.getBalance(), "Final balance must exactly equal 1250.0000 with zero lost updates");
        assertEquals(10, transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).size(), "Exactly 10 transactions recorded");
    }

    @Test
    @DisplayName("Concurrency & Idempotency: 10 parallel duplicate requests credit balance exactly once")
    void testConcurrentDuplicateReplaySafety() throws Exception {
        String userId = "USER_CONC_DUP_REPLAY";
        walletRepository.saveAndFlush(new Wallet(userId, new BigDecimal("500.0000")));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger duplicateDetectedCount = new AtomicInteger(0);
        AtomicInteger freshProcessedCount = new AtomicInteger(0);

        TransactionRequest req = new TransactionRequest(
                "TXN_REPLAY_PARALLEL_ID", userId, new BigDecimal("200.0000"), TransactionType.CREDIT
        );
        String requestJson = objectMapper.writeValueAsString(req);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/transactions/process")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                            .andReturn();

                    if (result.getResponse().getStatus() == 200) {
                        TransactionResponse res = objectMapper.readValue(
                                result.getResponse().getContentAsString(), TransactionResponse.class
                        );
                        if (res.getMessage() != null && res.getMessage().contains("Duplicate transaction detected")) {
                            duplicateDetectedCount.incrementAndGet();
                        } else {
                            freshProcessedCount.incrementAndGet();
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Duplicate stress test should complete within timeout");
        assertEquals(1, freshProcessedCount.get(), "Exactly 1 request should be fresh processed");
        assertEquals(9, duplicateDetectedCount.get(), "Exactly 9 requests should be detected as duplicates");

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertEquals(new BigDecimal("700.0000"), wallet.getBalance(), "Balance must be credited exactly once (500 + 200 = 700)");

        List<TransactionRecord> records = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertEquals(1, records.size(), "Database must store exactly one transaction record for TXN_REPLAY_PARALLEL_ID");
    }

    @Test
    @DisplayName("Fault Tolerance: Failed debit leaves wallet balance intact and unblocks future transactions")
    void testFailedDebitDoesNotAlterBalanceOrBlockFutureCredits() throws Exception {
        String userId = "USER_FAIL_SAFE";
        walletRepository.saveAndFlush(new Wallet(userId, new BigDecimal("100.0000")));

        // 1. Attempt Debit of 500.0000 against 100.0000
        TransactionRequest failDebit = new TransactionRequest(
                "TXN_FAIL_01", userId, new BigDecimal("500.0000"), TransactionType.DEBIT
        );
        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failDebit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Insufficient wallet balance")));

        // 2. Verify balance untouched
        mockMvc.perform(get("/api/v1/wallets/{userId}/balance", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(100.0000)));

        // 3. Subsequent valid Credit of 250.0000 succeeds
        TransactionRequest validCredit = new TransactionRequest(
                "TXN_PASS_01", userId, new BigDecimal("250.0000"), TransactionType.CREDIT
        );
        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCredit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.balanceAfter", is(350.0000)));

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertEquals(new BigDecimal("350.0000"), wallet.getBalance());
    }

    @Test
    @DisplayName("Arithmetic Precision: High-precision increments maintain BigDecimal scale 4 HALF_UP without drift")
    void testPrecisionAndRoundingEdgeCases() throws Exception {
        String userId = "USER_PRECISION_01";
        walletRepository.saveAndFlush(new Wallet(userId, BigDecimal.ZERO));

        // Credit 0.0001 ten times
        for (int i = 1; i <= 10; i++) {
            TransactionRequest req = new TransactionRequest(
                    "TXN_PRECISION_" + i, userId, new BigDecimal("0.0001"), TransactionType.CREDIT
            );
            mockMvc.perform(post("/api/v1/transactions/process")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        Wallet afterSmallCredits = walletRepository.findByUserId(userId).orElseThrow();
        assertEquals(new BigDecimal("0.0010"), afterSmallCredits.getBalance());

        // Credit 123.4567
        TransactionRequest reqCredit = new TransactionRequest(
                "TXN_PRECISION_BIG", userId, new BigDecimal("123.4567"), TransactionType.CREDIT
        );
        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqCredit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter", is(123.4577)));

        // Debit 23.4567
        TransactionRequest reqDebit = new TransactionRequest(
                "TXN_PRECISION_DEBIT", userId, new BigDecimal("23.4567"), TransactionType.DEBIT
        );
        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDebit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter", is(100.0010)));
    }
}
