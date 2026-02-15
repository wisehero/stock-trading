package com.wisehero.stocktrading.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for account position.
 */
@Embeddable
public class PositionId implements Serializable {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    protected PositionId() {
    }

    public PositionId(Long accountId, String symbol) {
        this.accountId = accountId;
        this.symbol = symbol;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PositionId that)) {
            return false;
        }
        return Objects.equals(accountId, that.accountId) && Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, symbol);
    }
}
