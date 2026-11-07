package com.payguard.controller;

import com.payguard.dto.CreateWalletRequest;
import com.payguard.dto.WalletBalanceResponse;
import com.payguard.dto.WalletResponse;
import com.payguard.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Create a new wallet for a user.
     * POST /api/v1/wallets
     */
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch wallet details by user ID.
     * GET /api/v1/wallets/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUserId(@PathVariable String userId) {
        WalletResponse response = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check wallet balance by user ID.
     * GET /api/v1/wallets/{userId}/balance
     */
    @GetMapping("/{userId}/balance")
    public ResponseEntity<WalletBalanceResponse> getBalanceByUserId(@PathVariable String userId) {
        WalletBalanceResponse response = walletService.getBalanceByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetch wallet details by unique wallet ID.
     * GET /api/v1/wallets/id/{walletId}
     */
    @GetMapping("/id/{walletId}")
    public ResponseEntity<WalletResponse> getWalletByWalletId(@PathVariable String walletId) {
        WalletResponse response = walletService.getWalletByWalletId(walletId);
        return ResponseEntity.ok(response);
    }
}

