package com.payguard.service;

import com.payguard.dto.TransactionRequest;
import com.payguard.dto.TransactionResponse;
import com.payguard.entity.TransactionRecord;
import com.payguard.entity.TransactionStatus;
import com.payguard.entity.TransactionType;
import com.payguard.entity.Wallet;
import com.payguard.exception.InsufficientBalanceException;
import com.payguard.exception.WalletNotFoundException;
import com.payguard.repository.TransactionRepository;
import com.payguard.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Processes a payment transaction (CREDIT or DEBIT) against a user's wallet.
     * Manages transactional boundaries, balance calculations, and audit history.
     */
    @Transactional
    public TransactionResponse process(TransactionRequest request) {
        Wallet wallet = walletRepository.findByUserIdWithLock(request.getUserId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for userId: " + request.getUserId()));

        BigDecimal balanceBefore = wallet.getBalance();

        if (request.getType() == TransactionType.DEBIT) {
            if (!wallet.hasSufficientBalance(request.getAmount())) {
                throw new InsufficientBalanceException(
                        "Insufficient wallet balance for userId: " + request.getUserId()
                                + ". Current balance: " + balanceBefore
                                + ", Debit requested: " + request.getAmount()
                );
            }
            wallet.debit(request.getAmount());
        } else if (request.getType() == TransactionType.CREDIT) {
            wallet.credit(request.getAmount());
        }

        walletRepository.save(wallet);

        BigDecimal balanceAfter = wallet.getBalance();

        TransactionRecord record = new TransactionRecord(
                request.getTransactionId(),
                wallet.getWalletId(),
                request.getUserId(),
                request.getAmount(),
                request.getType(),
                TransactionStatus.SUCCESS,
                balanceBefore,
                balanceAfter,
                null
        );

        transactionRepository.saveAndFlush(record);

        return new TransactionResponse(
                record.getTransactionId(),
                record.getStatus(),
                balanceAfter,
                "Transaction processed successfully."
        );
    }

    /**
     * Fetches chronological transaction history for a specific user.
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> getTransactionHistoryByUserId(String userId) {
        if (!walletRepository.existsByUserId(userId)) {
            throw new WalletNotFoundException("Wallet not found for userId: " + userId);
        }
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Retrieves details of a specific transaction by transaction ID.
     */
    @Transactional(readOnly = true)
    public TransactionRecord getTransactionByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for transactionId: " + transactionId));
    }
}
