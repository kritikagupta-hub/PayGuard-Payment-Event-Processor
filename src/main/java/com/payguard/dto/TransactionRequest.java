package com.payguard.dto;

import com.payguard.entity.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class TransactionRequest {

    @NotBlank(message = "transactionId is required and cannot be blank")
    private String transactionId;

    @NotBlank(message = "userId is required and cannot be blank")
    private String userId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be strictly greater than zero")
    @Digits(integer = 15, fraction = 4, message = "amount precision cannot exceed 4 decimal places")
    private BigDecimal amount;

    @NotNull(message = "type is required (must be CREDIT or DEBIT)")
    private TransactionType type;

    public TransactionRequest() {
    }

    public TransactionRequest(String transactionId, String userId, BigDecimal amount, TransactionType type) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}
