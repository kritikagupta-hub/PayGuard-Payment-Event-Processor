package com.payguard.dto;

import java.math.BigDecimal;

public class WalletBalanceResponse {

    private String walletId;
    private String userId;
    private BigDecimal balance;

    public WalletBalanceResponse() {
    }

    public WalletBalanceResponse(String walletId, String userId, BigDecimal balance) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}

