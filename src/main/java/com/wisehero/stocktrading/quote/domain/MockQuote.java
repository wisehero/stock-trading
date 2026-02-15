package com.wisehero.stocktrading.quote.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Fixed quote used by internal Mock Exchange.
 */
@Entity
@Table(name = "mock_quotes")
public class MockQuote {

    @Id
    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "available_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableQuantity;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected MockQuote() {
    }

    public static MockQuote create(String symbol, BigDecimal price, BigDecimal availableQuantity) {
        MockQuote quote = new MockQuote();
        quote.symbol = symbol;
        quote.price = price;
        quote.availableQuantity = availableQuantity;
        return quote;
    }

    public void update(BigDecimal price, BigDecimal availableQuantity) {
        this.price = price;
        this.availableQuantity = availableQuantity;
    }

    public BigDecimal consumeAvailableQuantity(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        BigDecimal consumed = quantity.min(availableQuantity);
        availableQuantity = availableQuantity.subtract(consumed);
        return consumed;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }
}
