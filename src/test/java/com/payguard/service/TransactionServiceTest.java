package com.payguard.service;

import com.payguard.dto.TransactionRequest;
import com.payguard.dto.TransactionResponse;
import com.payguard.entity.TransactionRecord;
import com.payguard.entity.TransactionStatus;
import com.payguard.entity.TransactionType;
import com.payguard.entity.Wallet;
import com.payguard.exception.DuplicateTransactionException;
import com.payguard.exception.InsufficientBalanceException;
import com.payguard.exception.WalletNotFoundException;
import com.payguard.repository.TransactionRepository;
import com.payguard.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(walletRepository, transactionRepository);
    }

    @Test
    @DisplayName("Should process CREDIT transaction and update balance")
    void testProcessCreditSuccess() {
        Wallet wallet = new Wallet("WLT-01", "USER_TXN_01", new BigDecimal("100.00"));
        TransactionRequest request = new TransactionRequest("TXN_01", "USER_TXN_01", new BigDecimal("50.00"), TransactionType.CREDIT);

        when(transactionRepository.findByTransactionId("TXN_01")).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdWithLock("USER_TXN_01")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.saveAndFlush(any(TransactionRecord.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = transactionService.process(request);

        assertNotNull(response);
        assertEquals("TXN_01", response.getTransactionId());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("150.0000"), response.getBalanceAfter());

        verify(walletRepository, times(1)).save(wallet);
        verify(transactionRepository, times(1)).saveAndFlush(any(TransactionRecord.class));
    }

    @Test
    @DisplayName("Should process DEBIT transaction and deduct balance")
    void testProcessDebitSuccess() {
        Wallet wallet = new Wallet("WLT-02", "USER_TXN_02", new BigDecimal("300.00"));
        TransactionRequest request = new TransactionRequest("TXN_02", "USER_TXN_02", new BigDecimal("100.00"), TransactionType.DEBIT);

        when(transactionRepository.findByTransactionId("TXN_02")).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdWithLock("USER_TXN_02")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.saveAndFlush(any(TransactionRecord.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = transactionService.process(request);

        assertNotNull(response);
        assertEquals("TXN_02", response.getTransactionId());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("200.0000"), response.getBalanceAfter());

        verify(walletRepository, times(1)).save(wallet);
        verify(transactionRepository, times(1)).saveAndFlush(any(TransactionRecord.class));
    }

    @Test
    @DisplayName("Idempotency Layer 1: Should return original transaction without updating balance when duplicate transactionId is received")
    void testProcessDuplicateTransactionReturnsExistingResultWithoutMutatingBalance() {
        TransactionRequest request = new TransactionRequest("TXN_DUP_FAST", "USER_TXN_DUP", new BigDecimal("100.00"), TransactionType.CREDIT);
        TransactionRecord existingRecord = new TransactionRecord(
                "TXN_DUP_FAST", "WLT-DUP", "USER_TXN_DUP", new BigDecimal("100.00"),
                TransactionType.CREDIT, TransactionStatus.SUCCESS, new BigDecimal("200.00"), new BigDecimal("300.00"), null
        );

        when(transactionRepository.findByTransactionId("TXN_DUP_FAST")).thenReturn(Optional.of(existingRecord));

        TransactionResponse response = transactionService.process(request);

        assertNotNull(response);
        assertEquals("TXN_DUP_FAST", response.getTransactionId());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("300.0000"), response.getBalanceAfter());
        assertTrue(response.getMessage().contains("Duplicate transaction detected"));

        verify(walletRepository, never()).findByUserIdWithLock(anyString());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).saveAndFlush(any(TransactionRecord.class));
    }

    @Test
    @DisplayName("Idempotency Layer 2: Should throw DuplicateTransactionException when concurrent insert violates DB unique constraint")
    void testProcessConcurrentDuplicateThrowsDuplicateTransactionException() {
        Wallet wallet = new Wallet("WLT-CONC", "USER_CONC", new BigDecimal("500.00"));
        TransactionRequest request = new TransactionRequest("TXN_CONC", "USER_CONC", new BigDecimal("100.00"), TransactionType.DEBIT);

        when(transactionRepository.findByTransactionId("TXN_CONC")).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdWithLock("USER_CONC")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.saveAndFlush(any(TransactionRecord.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        assertThrows(DuplicateTransactionException.class, () -> transactionService.process(request));
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when debit exceeds balance")
    void testProcessDebitInsufficientBalance() {
        Wallet wallet = new Wallet("WLT-03", "USER_TXN_03", new BigDecimal("50.00"));
        TransactionRequest request = new TransactionRequest("TXN_03", "USER_TXN_03", new BigDecimal("100.00"), TransactionType.DEBIT);

        when(transactionRepository.findByTransactionId("TXN_03")).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdWithLock("USER_TXN_03")).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.process(request));

        verify(walletRepository, never()).save(wallet);
        verify(transactionRepository, never()).saveAndFlush(any(TransactionRecord.class));
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet does not exist")
    void testProcessWalletNotFound() {
        TransactionRequest request = new TransactionRequest("TXN_04", "NON_EXISTENT", new BigDecimal("100.00"), TransactionType.CREDIT);

        when(transactionRepository.findByTransactionId("TXN_04")).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdWithLock("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> transactionService.process(request));
    }

    @Test
    @DisplayName("Should retrieve transaction history for user")
    void testGetTransactionHistory() {
        when(walletRepository.existsByUserId("USER_TXN_05")).thenReturn(true);
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc("USER_TXN_05"))
                .thenReturn(List.of(
                        new TransactionRecord("TXN_05", "WLT-05", "USER_TXN_05", new BigDecimal("50.00"), TransactionType.CREDIT, TransactionStatus.SUCCESS, BigDecimal.ZERO, new BigDecimal("50.00"), null)
                ));

        List<TransactionRecord> history = transactionService.getTransactionHistoryByUserId("USER_TXN_05");

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("TXN_05", history.get(0).getTransactionId());
    }
}
