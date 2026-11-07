package com.payguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payguard.dto.CreateWalletRequest;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/wallets should create wallet and return 201 Created")
    void testCreateWalletApiSuccess() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("USER_API_01", new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.walletId", notNullValue()))
                .andExpect(jsonPath("$.userId", is("USER_API_01")))
                .andExpect(jsonPath("$.balance", is(1000.0000)));
    }

    @Test
    @DisplayName("POST /api/v1/wallets should return 400 Bad Request when userId is blank")
    void testCreateWalletValidationFailure() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("", new BigDecimal("100.00"));

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("POST /api/v1/wallets should return 409 Conflict when user already has a wallet")
    void testCreateWalletDuplicateConflict() throws Exception {
        walletRepository.saveAndFlush(new Wallet("USER_API_DUP", new BigDecimal("500.00")));

        CreateWalletRequest request = new CreateWalletRequest("USER_API_DUP", new BigDecimal("200.00"));

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("GET /api/v1/wallets/{userId} should return wallet details with 200 OK")
    void testGetWalletByUserIdSuccess() throws Exception {
        Wallet saved = walletRepository.saveAndFlush(new Wallet("USER_API_GET", new BigDecimal("350.50")));

        mockMvc.perform(get("/api/v1/wallets/USER_API_GET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId", is(saved.getWalletId())))
                .andExpect(jsonPath("$.userId", is("USER_API_GET")))
                .andExpect(jsonPath("$.balance", is(350.5000)));
    }

    @Test
    @DisplayName("GET /api/v1/wallets/{userId} should return 404 Not Found for non-existing wallet")
    void testGetWalletByUserIdNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/NON_EXISTENT_USER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("GET /api/v1/wallets/{userId}/balance should return balance with 200 OK")
    void testGetBalanceByUserIdSuccess() throws Exception {
        Wallet saved = walletRepository.saveAndFlush(new Wallet("USER_API_BAL", new BigDecimal("750.25")));

        mockMvc.perform(get("/api/v1/wallets/USER_API_BAL/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId", is(saved.getWalletId())))
                .andExpect(jsonPath("$.userId", is("USER_API_BAL")))
                .andExpect(jsonPath("$.balance", is(750.2500)));
    }
}

