package com.payguard.service;

import com.payguard.dto.CreateWalletRequest;
import com.payguard.dto.WalletBalanceResponse;
import com.payguard.dto.WalletResponse;
import com.payguard.entity.Wallet;
import com.payguard.exception.WalletAlreadyExistsException;
import com.payguard.exception.WalletNotFoundException;
import com.payguard.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Creates a new wallet for a user.
     * Generates a unique wallet ID and sets initial balance.
     */
    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        if (walletRepository.existsByUserId(request.getUserId())) {
            throw new WalletAlreadyExistsException("Wallet already exists for userId: " + request.getUserId());
        }

        BigDecimal initialBalance = request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO;
        Wallet wallet = new Wallet(request.getUserId(), initialBalance);
        Wallet saved = walletRepository.save(wallet);

        return mapToResponse(saved);
    }

    /**
     * Retrieves wallet details by user ID.
     */
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for userId: " + userId));
        return mapToResponse(wallet);
    }

    /**
     * Retrieves wallet details by wallet ID.
     */
    @Transactional(readOnly = true)
    public WalletResponse getWalletByWalletId(String walletId) {
        Wallet wallet = walletRepository.findByWalletId(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for walletId: " + walletId));
        return mapToResponse(wallet);
    }

    /**
     * Retrieves current balance for a user's wallet.
     */
    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalanceByUserId(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for userId: " + userId));
        return new WalletBalanceResponse(wallet.getWalletId(), wallet.getUserId(), wallet.getBalance());
    }

    /**
     * Retrieves current balance by wallet ID.
     */
    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalanceByWalletId(String walletId) {
        Wallet wallet = walletRepository.findByWalletId(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for walletId: " + walletId));
        return new WalletBalanceResponse(wallet.getWalletId(), wallet.getUserId(), wallet.getBalance());
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getWalletId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}

