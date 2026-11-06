package com.payguard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "transaction_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transaction_record_txn_id", columnNames = {"transaction_id"})
        },
        indexes = {
                @Index(name = "idx_txn_transaction_id", columnList = "transaction_id", unique = true),
                @Index(name = "idx_txn_user_id", columnList = "user_id"),
                @Index(name = "idx_txn_wallet_id", columnList = "wallet_id")
        }
)
public class TransactionRecord {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, updatable = false, length = 64)
    private String transactionId;

    @Column(name = "wallet_id", nullable = false, updatable = false, length = 64)
    private String walletId;

    @Column(name = "user_id", nullable = false, updatable = false, length = 64)
    private String userId;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 16)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TransactionStatus status;

    @Column(name = "balance_before", precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TransactionRecord() {
    }

    public TransactionRecord(String transactionId, String walletId, String userId,
                             BigDecimal amount, TransactionType type, TransactionStatus status,
                             BigDecimal balanceBefore, BigDecimal balanceAfter, String failureReason) {
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.userId = userId;
        this.amount = amount != null ? amount.setScale(SCALE, ROUNDING_MODE) : BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        this.type = type;
        this.status = status;
        this.balanceBefore = balanceBefore != null ? balanceBefore.setScale(SCALE, ROUNDING_MODE) : null;
        this.balanceAfter = balanceAfter != null ? balanceAfter.setScale(SCALE, ROUNDING_MODE) : null;
        this.failureReason = failureReason;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.amount != null) {
            this.amount = this.amount.setScale(SCALE, ROUNDING_MODE);
        }
        if (this.balanceBefore != null) {
            this.balanceBefore = this.balanceBefore.setScale(SCALE, ROUNDING_MODE);
        }
        if (this.balanceAfter != null) {
            this.balanceAfter = this.balanceAfter.setScale(SCALE, ROUNDING_MODE);
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(SCALE, ROUNDING_MODE) : null;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore != null ? balanceBefore.setScale(SCALE, ROUNDING_MODE) : null;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter != null ? balanceAfter.setScale(SCALE, ROUNDING_MODE) : null;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionRecord that = (TransactionRecord) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", walletId='" + walletId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", type=" + type +
                ", status=" + status +
                ", balanceBefore=" + balanceBefore +
                ", balanceAfter=" + balanceAfter +
                ", failureReason='" + failureReason + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
