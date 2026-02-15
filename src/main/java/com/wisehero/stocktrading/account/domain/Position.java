package com.wisehero.stocktrading.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Position snapshot with hold tracking for sell orders.
 */
@Entity
@Table(name = "positions")
public class Position {

    private static final int PRICE_SCALE = 4;

    @EmbeddedId
    private PositionId id;

    @Column(name = "available_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableQuantity;

    @Column(name = "held_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal heldQuantity;

    @Column(name = "average_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal averagePrice;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Position() {
    }

    public static Position create(Long accountId, String symbol, BigDecimal availableQuantity, BigDecimal averagePrice) {
        Position position = new Position();
        position.id = new PositionId(accountId, symbol);
        position.availableQuantity = availableQuantity;
        position.heldQuantity = BigDecimal.ZERO;
        position.averagePrice = averagePrice;
        return position;
    }

    public void hold(BigDecimal quantity) {
        validatePositive(quantity);
        if (availableQuantity.compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient available quantity");
        }
        availableQuantity = availableQuantity.subtract(quantity);
        heldQuantity = heldQuantity.add(quantity);
    }

    public void consumeHeld(BigDecimal quantity) {
        validatePositive(quantity);
        if (heldQuantity.compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient held quantity");
        }
        heldQuantity = heldQuantity.subtract(quantity);
    }

    public void releaseHeld(BigDecimal quantity) {
        validatePositive(quantity);
        if (heldQuantity.compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient held quantity");
        }
        heldQuantity = heldQuantity.subtract(quantity);
        availableQuantity = availableQuantity.add(quantity);
    }

    public void addBoughtQuantity(BigDecimal quantity, BigDecimal price) {
        validatePositive(quantity);
        validatePositive(price);

        BigDecimal currentValue = averagePrice.multiply(availableQuantity);
        BigDecimal newValue = price.multiply(quantity);
        BigDecimal newTotalQuantity = availableQuantity.add(quantity);

        this.averagePrice = currentValue
                .add(newValue)
                .divide(newTotalQuantity, PRICE_SCALE, RoundingMode.HALF_UP);
        this.availableQuantity = newTotalQuantity;
    }

    public void updateForMock(BigDecimal availableQuantity, BigDecimal averagePrice) {
        validateNonNegative(availableQuantity);
        validateNonNegative(averagePrice);
        this.availableQuantity = availableQuantity;
        this.heldQuantity = BigDecimal.ZERO;
        this.averagePrice = averagePrice;
    }

    private void validatePositive(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Value must be positive");
        }
    }

    private void validateNonNegative(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Value must be non-negative");
        }
    }

    public PositionId getId() {
        return id;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }

    public BigDecimal getHeldQuantity() {
        return heldQuantity;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }
}
