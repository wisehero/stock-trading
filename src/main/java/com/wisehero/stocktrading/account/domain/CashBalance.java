package com.wisehero.stocktrading.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Account cash snapshot with hold tracking.
 */
@Entity
@Table(name = "cash_balances")
public class CashBalance {

    @Id
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "available_cash", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableCash;

    @Column(name = "held_cash", nullable = false, precision = 19, scale = 4)
    private BigDecimal heldCash;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected CashBalance() {
    }

    public static CashBalance create(Long accountId, BigDecimal availableCash) {
        CashBalance balance = new CashBalance();
        balance.accountId = accountId;
        balance.availableCash = availableCash;
        balance.heldCash = BigDecimal.ZERO;
        return balance;
    }

    public void hold(BigDecimal amount) {
        validatePositive(amount);
        if (availableCash.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available cash");
        }
        availableCash = availableCash.subtract(amount);
        heldCash = heldCash.add(amount);
    }

    public void consumeHeld(BigDecimal amount) {
        validatePositive(amount);
        if (heldCash.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient held cash");
        }
        heldCash = heldCash.subtract(amount);
    }

    public void releaseHeld(BigDecimal amount) {
        validatePositive(amount);
        if (heldCash.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient held cash");
        }
        heldCash = heldCash.subtract(amount);
        availableCash = availableCash.add(amount);
    }

    public void addAvailable(BigDecimal amount) {
        validatePositive(amount);
        availableCash = availableCash.add(amount);
    }

    public void updateForMock(BigDecimal availableCash) {
        validateNonNegative(availableCash);
        this.availableCash = availableCash;
        this.heldCash = BigDecimal.ZERO;
    }

    private void validatePositive(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void validateNonNegative(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getAvailableCash() {
        return availableCash;
    }

    public BigDecimal getHeldCash() {
        return heldCash;
    }
}
