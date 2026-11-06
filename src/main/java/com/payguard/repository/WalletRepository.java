package com.payguard.repository;

import com.payguard.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByWalletId(String walletId);

    Optional<Wallet> findByUserId(String userId);

    boolean existsByUserId(String userId);

    boolean existsByWalletId(String walletId);

    /**
     * Acquires a pessimistic write lock (SELECT ... FOR UPDATE) on the wallet row
     * by userId to ensure safe concurrent balance updates and prevent race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdWithLock(@Param("userId") String userId);

    /**
     * Acquires a pessimistic write lock (SELECT ... FOR UPDATE) on the wallet row
     * by walletId.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletId = :walletId")
    Optional<Wallet> findByWalletIdWithLock(@Param("walletId") String walletId);
}
