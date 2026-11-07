package com.payguard.service;

import com.payguard.dto.CreateWalletRequest;
import com.payguard.dto.WalletBalanceResponse;
import com.payguard.dto.WalletResponse;
import com.payguard.entity.Wallet;
import com.payguard.exception.WalletAlreadyExistsException;
import com.payguard.exception.WalletNotFoundException;
import com.payguard.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository);
    }

    @Test
    @DisplayName("Should create wallet successfully when user does not have an existing wallet")
    void testCreateWalletSuccess() {
        CreateWalletRequest request = new CreateWalletRequest("USER_SRV_01", new BigDecimal("500.00"));
        Wallet mockSaved = new Wallet("WLT-TEST-01", "USER_SRV_01", new BigDecimal("500.00"));

        when(walletRepository.existsByUserId("USER_SRV_01")).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(mockSaved);

        WalletResponse response = walletService.createWallet(request);

        assertNotNull(response);
        assertEquals("WLT-TEST-01", response.getWalletId());
        assertEquals("USER_SRV_01", response.getUserId());
        assertEquals(new BigDecimal("500.0000"), response.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw WalletAlreadyExistsException when user already has a wallet")
    void testCreateWalletAlreadyExistsThrowsException() {
        CreateWalletRequest request = new CreateWalletRequest("USER_SRV_01", new BigDecimal("100.00"));

        when(walletRepository.existsByUserId("USER_SRV_01")).thenReturn(true);

        assertThrows(WalletAlreadyExistsException.class, () -> walletService.createWallet(request));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should retrieve wallet details by userId successfully")
    void testGetWalletByUserIdSuccess() {
        Wallet mockWallet = new Wallet("WLT-TEST-02", "USER_SRV_02", new BigDecimal("250.00"));

        when(walletRepository.findByUserId("USER_SRV_02")).thenReturn(Optional.of(mockWallet));

        WalletResponse response = walletService.getWalletByUserId("USER_SRV_02");

        assertNotNull(response);
        assertEquals("WLT-TEST-02", response.getWalletId());
        assertEquals("USER_SRV_02", response.getUserId());
        assertEquals(new BigDecimal("250.0000"), response.getBalance());
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet not found for userId")
    void testGetWalletByUserIdNotFoundThrowsException() {
        when(walletRepository.findByUserId("UNKNOWN_USER")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletByUserId("UNKNOWN_USER"));
    }

    @Test
    @DisplayName("Should retrieve wallet balance by userId successfully")
    void testGetBalanceByUserIdSuccess() {
        Wallet mockWallet = new Wallet("WLT-TEST-03", "USER_SRV_03", new BigDecimal("750.50"));

        when(walletRepository.findByUserId("USER_SRV_03")).thenReturn(Optional.of(mockWallet));

        WalletBalanceResponse response = walletService.getBalanceByUserId("USER_SRV_03");

        assertNotNull(response);
        assertEquals("WLT-TEST-03", response.getWalletId());
        assertEquals("USER_SRV_03", response.getUserId());
        assertEquals(new BigDecimal("750.5000"), response.getBalance());
    }
}

