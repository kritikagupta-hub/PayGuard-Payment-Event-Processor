package com.payguard.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class CreateWalletRequest {

    @NotBlank(message = "userId is required and cannot be blank")
    private String userId;

    @PositiveOrZero(message = "initialBalance must be greater than or equal to zero")
    @Digits(integer = 15, fraction = 4, message = "initialBalance precision cannot exceed 4 decimal places")
    private BigDecimal initialBalance;

    public CreateWalletRequest() {
    }

    public CreateWalletRequest(String userId, BigDecimal initialBalance) {
        this.userId = userId;
        this.initialBalance = initialBalance;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}
