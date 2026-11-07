package com.payguard.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payguard.dto.CreateWalletRequest;
import com.payguard.dto.TransactionRequest;
import com.payguard.entity.TransactionType;
import com.payguard.entity.Wallet;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("Validation: POST /api/v1/wallets with blank userId returns 400 and validationErrors map")
    void testCreateWalletBlankUserIdValidation() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("", new BigDecimal("100.00"));

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors.userId", notNullValue()));
    }

    @Test
    @DisplayName("Validation: POST /api/v1/wallets with negative initial balance returns 400")
    void testCreateWalletNegativeInitialBalanceValidation() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("USER_NEG_BAL", new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.initialBalance", notNullValue()));
    }

    @Test
    @DisplayName("Validation: POST /api/v1/wallets with scale > 4 decimals returns 400")
    void testCreateWalletExcessiveFractionValidation() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("USER_SCALE", new BigDecimal("10.12345"));

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.initialBalance", containsString("precision cannot exceed 4 decimal places")));
    }

    @Test
    @DisplayName("Validation: POST /api/v1/transactions/process with missing fields returns 400 and all field errors")
    void testProcessTransactionMissingFieldsValidation() throws Exception {
        String emptyJson = "{}";

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors.transactionId", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.userId", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.amount", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.type", notNullValue()));
    }

    @Test
    @DisplayName("Validation: POST /api/v1/transactions/process with zero amount returns 400")
    void testProcessTransactionZeroAmountValidation() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "TXN_ZERO_AMT", "USER_VAL_1", BigDecimal.ZERO, TransactionType.CREDIT
        );

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.amount", containsString("must be strictly greater than zero")));
    }

    @Test
    @DisplayName("Validation: POST /api/v1/transactions/process with negative amount returns 400")
    void testProcessTransactionNegativeAmountValidation() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "TXN_NEG_AMT", "USER_VAL_1", new BigDecimal("-25.00"), TransactionType.CREDIT
        );

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.amount", containsString("must be strictly greater than zero")));
    }

    @Test
    @DisplayName("Validation: POST /api/v1/transactions/process with amount scale > 4 returns 400")
    void testProcessTransactionExcessiveFractionValidation() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "TXN_SCALE_AMT", "USER_VAL_1", new BigDecimal("50.12345"), TransactionType.CREDIT
        );

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.amount", containsString("precision cannot exceed 4 decimal places")));
    }

    @Test
    @DisplayName("Exception Handling: POST /api/v1/transactions/process with invalid enum type returns 400")
    void testProcessTransactionInvalidEnumType() throws Exception {
        String invalidJson = """
                {
                    "transactionId": "TXN_INVALID_ENUM",
                    "userId": "USER_VAL_1",
                    "amount": 100.00,
                    "type": "INVALID_ENUM_VALUE"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Allowed values are CREDIT or DEBIT")));
    }

    @Test
    @DisplayName("Exception Handling: POST /api/v1/transactions/process with malformed JSON syntax returns 400")
    void testProcessTransactionMalformedJsonSyntax() throws Exception {
        String brokenJson = "{\"transactionId\": \"TXN_BROKEN\", \"userId\": ";

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brokenJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Malformed JSON request")));
    }

    @Test
    @DisplayName("Exception Handling: DELETE on POST-only endpoint returns 405 Method Not Allowed")
    void testUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(delete("/api/v1/wallets"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.error", is("Method Not Allowed")));
    }

    @Test
    @DisplayName("Exception Handling: Non-existent endpoint returns 404 Not Found")
    void testNonExistentEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("Exception Handling: GET non-existent wallet returns 404 WalletNotFoundException")
    void testWalletNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/USER_DOES_NOT_EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Wallet not found for userId: USER_DOES_NOT_EXIST")));
    }

    @Test
    @DisplayName("Exception Handling: Create duplicate wallet returns 409 WalletAlreadyExistsException")
    void testWalletAlreadyExists() throws Exception {
        walletRepository.saveAndFlush(new Wallet("USER_DUP_EX", new BigDecimal("100.00")));

        CreateWalletRequest request = new CreateWalletRequest("USER_DUP_EX", new BigDecimal("50.00"));

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Wallet already exists for userId: USER_DUP_EX")));
    }

    @Test
    @DisplayName("Exception Handling: Debit exceeding balance returns 400 InsufficientBalanceException")
    void testInsufficientBalance() throws Exception {
        walletRepository.saveAndFlush(new Wallet("USER_INSUFFICIENT", new BigDecimal("20.00")));

        TransactionRequest request = new TransactionRequest(
                "TXN_INSUFFICIENT_EX", "USER_INSUFFICIENT", new BigDecimal("50.00"), TransactionType.DEBIT
        );

        mockMvc.perform(post("/api/v1/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Insufficient wallet balance for userId: USER_INSUFFICIENT")));
    }
}
