package com.payguard.dto;

import com.payguard.entity.TransactionStatus;

import java.math.BigDecimal;

public class TransactionResponse {

    private String transactionId;
    private TransactionStatus status;
    private BigDecimal balanceAfter;
    private String message;

    public TransactionResponse() {
    }

    public TransactionResponse(String transactionId, TransactionStatus status, BigDecimal balanceAfter, String message) {
        this.transactionId = transactionId;
        this.status = status;
        this.balanceAfter = balanceAfter;
        this.message = message;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
