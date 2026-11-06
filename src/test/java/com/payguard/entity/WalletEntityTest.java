package com.payguard.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WalletEntityTest {

    @Test
    @DisplayName("Should credit money to wallet balance with BigDecimal precision")
    void testCreditWallet() {
        Wallet wallet = new Wallet("USER_01", new BigDecimal("100.00"));
        wallet.credit(new BigDecimal("50.25"));

        assertEquals(new BigDecimal("150.2500"), wallet.getBalance());
    }

    @Test
    @DisplayName("Should debit money from wallet balance successfully")
    void testDebitWalletSuccess() {
        Wallet wallet = new Wallet("USER_02", new BigDecimal("200.00"));
        wallet.debit(new BigDecimal("75.50"));

        assertEquals(new BigDecimal("124.5000"), wallet.getBalance());
    }

    @Test
    @DisplayName("Should throw exception when debit amount exceeds balance")
    void testDebitInsufficientBalance() {
        Wallet wallet = new Wallet("USER_03", new BigDecimal("50.00"));

        assertThrows(IllegalStateException.class, () -> wallet.debit(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("Should throw exception when credit amount is negative or zero")
    void testInvalidCreditAmount() {
        Wallet wallet = new Wallet("USER_04", BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> wallet.credit(new BigDecimal("-10.00")));
        assertThrows(IllegalArgumentException.class, () -> wallet.credit(BigDecimal.ZERO));
    }
}
