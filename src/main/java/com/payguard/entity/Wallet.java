package com.payguard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_wallet_id", columnNames = {"wallet_id"}),
                @UniqueConstraint(name = "uk_wallet_user_id", columnNames = {"user_id"})
        },
        indexes = {
                @Index(name = "idx_wallet_wallet_id", columnList = "wallet_id", unique = true),
                @Index(name = "idx_wallet_user_id", columnList = "user_id", unique = true)
        }
)
public class Wallet {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false, unique = true, updatable = false, length = 64)
    private String walletId;

    @Column(name = "user_id", nullable = false, unique = true, length = 64)
    private String userId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Wallet() {
        this.balance = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
    }

    public Wallet(String userId, BigDecimal initialBalance) {
        this.userId = userId;
        this.walletId = "WLT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        this.balance = (initialBalance != null ? initialBalance : BigDecimal.ZERO).setScale(SCALE, ROUNDING_MODE);
    }

    public Wallet(String walletId, String userId, BigDecimal initialBalance) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = (initialBalance != null ? initialBalance : BigDecimal.ZERO).setScale(SCALE, ROUNDING_MODE);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.walletId == null || this.walletId.isBlank()) {
            this.walletId = "WLT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        } else {
            this.balance = this.balance.setScale(SCALE, ROUNDING_MODE);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.balance != null) {
            this.balance = this.balance.setScale(SCALE, ROUNDING_MODE);
        }
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be strictly greater than zero");
        }
        BigDecimal normalizedAmount = amount.setScale(SCALE, ROUNDING_MODE);
        this.balance = this.balance.add(normalizedAmount);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be strictly greater than zero");
        }
        BigDecimal normalizedAmount = amount.setScale(SCALE, ROUNDING_MODE);
        if (this.balance.compareTo(normalizedAmount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance for debit transaction");
        }
        this.balance = this.balance.subtract(normalizedAmount);
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return this.balance.compareTo(amount.setScale(SCALE, ROUNDING_MODE)) >= 0;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        this.balance = balance != null ? balance.setScale(SCALE, ROUNDING_MODE) : BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return Objects.equals(walletId, wallet.walletId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walletId);
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "id=" + id +
                ", walletId='" + walletId + '\'' +
                ", userId='" + userId + '\'' +
                ", balance=" + balance +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
