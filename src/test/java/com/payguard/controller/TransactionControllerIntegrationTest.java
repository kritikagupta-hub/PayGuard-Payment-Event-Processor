package com.payguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payguard.dto.TransactionRequest;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/transactions/process should process CREDIT transaction successfully")
    void testProcessCreditSuccess() throws Exception {
        walletRepository.saveAndFlush(new Wallet("USER_TXN_API_01", new BigDecimal("100.00")));

        TransactionRequest request = new TransactionRequest(
                "TXN_API_CREDIT_01",
                "USER_TXN_API_01",
                new BigDecimal("250.00"),
                TransactionType.CREDIT
        );

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is("TXN_API_CREDIT_01")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.balanceAfter", is(350.0000)))
                .andExpect(jsonPath("$.message", is("Transaction processed successfully.")));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/process should process DEBIT transaction successfully")
    void testProcessDebitSuccess() throws Exception {
        walletRepository.saveAndFlush(new Wallet("USER_TXN_API_02", new BigDecimal("500.00")));

        TransactionRequest request = new TransactionRequest(
                "TXN_API_DEBIT_01",
                "USER_TXN_API_02",
                new BigDecimal("150.00"),
                TransactionType.DEBIT
        );

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is("TXN_API_DEBIT_01")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.balanceAfter", is(350.0000)));
    }

    @Test
    @DisplayName("Idempotency: Repeated requests with identical transactionId must mutate balance exactly ONCE")
    void testIdempotencyDuplicateTransaction() throws Exception {
        walletRepository.saveAndFlush(new Wallet("USER_IDEM_01", new BigDecimal("500.00")));

        TransactionRequest request = new TransactionRequest(
                "TXN_IDEMPOTENT_100",
                "USER_IDEM_01",
                new BigDecimal("200.00"),
                TransactionType.CREDIT
        );

        // 1st Attempt: processes transaction and updates balance from 500 to 700
        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is("TXN_IDEMPOTENT_100")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.balanceAfter", is(700.0000)))
                .andExpect(jsonPath("$.message", is("Transaction processed successfully.")));

        // 2nd Attempt: identical transactionId sent again
        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is("TXN_IDEMPOTENT_100")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.balanceAfter", is(700.0000)))
                .andExpect(jsonPath("$.message", containsString("Duplicate transaction detected")));

        // 3rd Attempt: identical transactionId sent a 3rd time
        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter", is(700.0000)));

        // Verify wallet balance in DB is strictly 700.0000 (NOT 900 or 1100)
        Wallet wallet = walletRepository.findByUserId("USER_IDEM_01").orElseThrow();
        assertEquals(new BigDecimal("700.0000"), wallet.getBalance());

        // Verify only 1 transaction record exists in DB
        assertEquals(1, transactionRepository.findAll().size());
    }

    @Test
    @DisplayName("POST /api/v1/transactions/process should return 400 Bad Request on insufficient balance")
    void testProcessDebitInsufficientBalance() throws Exception {
        walletRepository.saveAndFlush(new Wallet("USER_TXN_API_03", new BigDecimal("50.00")));

        TransactionRequest request = new TransactionRequest(
                "TXN_API_INSUFFICIENT_01",
                "USER_TXN_API_03",
                new BigDecimal("200.00"),
                TransactionType.DEBIT
        );

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/process should return 404 Not Found for non-existing wallet")
    void testProcessWalletNotFound() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "TXN_API_NOT_FOUND_01",
                "UNKNOWN_USER",
                new BigDecimal("100.00"),
                TransactionType.CREDIT
        );

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/user/{userId} should return user transaction history")
    void testGetUserTransactionHistory() throws Exception {
        walletRepository.saveAndFlush(new Wallet("USER_TXN_API_HIST", new BigDecimal("500.00")));

        TransactionRequest request1 = new TransactionRequest("TXN_H1", "USER_TXN_API_HIST", new BigDecimal("100.00"), TransactionType.CREDIT);
        TransactionRequest request2 = new TransactionRequest("TXN_H2", "USER_TXN_API_HIST", new BigDecimal("50.00"), TransactionType.DEBIT);

        mockMvc.perform(post("/api/v1/transactions/process").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request1))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/transactions/process").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request2))).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/transactions/user/USER_TXN_API_HIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
