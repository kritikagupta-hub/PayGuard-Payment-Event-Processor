package com.payguard.repository;

import com.payguard.entity.TransactionRecord;
import com.payguard.entity.TransactionStatus;
import com.payguard.entity.TransactionType;
import com.payguard.entity.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RepositoryIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should save and find wallet by userId and walletId")
    void testSaveAndFindWallet() {
        Wallet wallet = new Wallet("USER_REPO_01", new BigDecimal("500.00"));
        Wallet saved = walletRepository.saveAndFlush(wallet);

        assertNotNull(saved.getId());
        assertNotNull(saved.getWalletId());
        assertEquals(new BigDecimal("500.0000"), saved.getBalance());

        Optional<Wallet> byUserId = walletRepository.findByUserId("USER_REPO_01");
        assertTrue(byUserId.isPresent());
        assertEquals(saved.getWalletId(), byUserId.get().getWalletId());

        Optional<Wallet> byWalletId = walletRepository.findByWalletId(saved.getWalletId());
        assertTrue(byWalletId.isPresent());
        assertEquals("USER_REPO_01", byWalletId.get().getUserId());

        assertTrue(walletRepository.existsByUserId("USER_REPO_01"));
        assertFalse(walletRepository.existsByUserId("NON_EXISTING_USER"));
    }

    @Test
    @DisplayName("Should find wallet with pessimistic write lock")
    void testFindWalletWithLock() {
        Wallet wallet = new Wallet("USER_REPO_LOCK", new BigDecimal("1000.00"));
        walletRepository.saveAndFlush(wallet);

        Optional<Wallet> lockedWallet = walletRepository.findByUserIdWithLock("USER_REPO_LOCK");
        assertTrue(lockedWallet.isPresent());
        assertEquals(new BigDecimal("1000.0000"), lockedWallet.get().getBalance());
    }

    @Test
    @DisplayName("Should fail with unique constraint violation when saving duplicate userId in wallets")
    void testWalletDuplicateUserIdConstraint() {
        Wallet wallet1 = new Wallet("USER_DUP", new BigDecimal("100.00"));
        walletRepository.saveAndFlush(wallet1);

        Wallet wallet2 = new Wallet("USER_DUP", new BigDecimal("200.00"));
        assertThrows(DataIntegrityViolationException.class, () -> {
            walletRepository.saveAndFlush(wallet2);
        });
    }

    @Test
    @DisplayName("Should save and query transaction records by transactionId and userId")
    void testSaveAndQueryTransactionRecord() {
        TransactionRecord record = new TransactionRecord(
                "TXN_REPO_001",
                "WLT_001",
                "USER_TXN_01",
                new BigDecimal("250.00"),
                TransactionType.CREDIT,
                TransactionStatus.SUCCESS,
                BigDecimal.ZERO,
                new BigDecimal("250.00"),
                null
        );

        transactionRepository.saveAndFlush(record);

        assertTrue(transactionRepository.existsByTransactionId("TXN_REPO_001"));
        assertFalse(transactionRepository.existsByTransactionId("TXN_UNKNOWN"));

        Optional<TransactionRecord> fetched = transactionRepository.findByTransactionId("TXN_REPO_001");
        assertTrue(fetched.isPresent());
        assertEquals(new BigDecimal("250.0000"), fetched.get().getAmount());
        assertEquals(TransactionType.CREDIT, fetched.get().getType());
        assertEquals(TransactionStatus.SUCCESS, fetched.get().getStatus());

        List<TransactionRecord> userHistory = transactionRepository.findByUserIdOrderByCreatedAtDesc("USER_TXN_01");
        assertEquals(1, userHistory.size());
        assertEquals("TXN_REPO_001", userHistory.get(0).getTransactionId());
    }

    @Test
    @DisplayName("Should enforce unique constraint on transactionId to guarantee idempotency")
    void testTransactionIdUniqueConstraint() {
        TransactionRecord record1 = new TransactionRecord(
                "TXN_IDEMPOTENT_01",
                "WLT_002",
                "USER_IDEM_01",
                new BigDecimal("100.00"),
                TransactionType.DEBIT,
                TransactionStatus.SUCCESS,
                new BigDecimal("500.00"),
                new BigDecimal("400.00"),
                null
        );
        transactionRepository.saveAndFlush(record1);

        TransactionRecord record2 = new TransactionRecord(
                "TXN_IDEMPOTENT_01",
                "WLT_002",
                "USER_IDEM_01",
                new BigDecimal("100.00"),
                TransactionType.DEBIT,
                TransactionStatus.SUCCESS,
                new BigDecimal("400.00"),
                new BigDecimal("300.00"),
                null
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionRepository.saveAndFlush(record2);
        });
    }
}
