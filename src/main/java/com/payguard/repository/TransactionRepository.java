package com.payguard.repository;

import com.payguard.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

    /**
     * Finds an existing transaction by its unique idempotency transaction ID.
     */
    Optional<TransactionRecord> findByTransactionId(String transactionId);

    /**
     * Checks if a transaction with the given ID already exists in the system.
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Retrieves chronological transaction history for a specific wallet.
     */
    List<TransactionRecord> findByWalletIdOrderByCreatedAtDesc(String walletId);

    /**
     * Retrieves chronological transaction history for a specific user.
     */
    List<TransactionRecord> findByUserIdOrderByCreatedAtDesc(String userId);
}
