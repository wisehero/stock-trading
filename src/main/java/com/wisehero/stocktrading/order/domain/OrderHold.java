package com.wisehero.stocktrading.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Reserved amount or quantity for an order.
 */
@Entity
@Table(name = "order_holds")
public class OrderHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hold_type", nullable = false, length = 20)
    private HoldType holdType;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "consumed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal consumedAmount;

    @Column(name = "released_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal releasedAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderHold() {
    }

    public static OrderHold create(Long orderId, Long accountId, HoldType holdType, BigDecimal totalAmount) {
        OrderHold hold = new OrderHold();
        hold.orderId = orderId;
        hold.accountId = accountId;
        hold.holdType = holdType;
        hold.totalAmount = totalAmount;
        hold.consumedAmount = BigDecimal.ZERO;
        hold.releasedAmount = BigDecimal.ZERO;
        return hold;
    }

    public void consume(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Consumed amount must be positive");
        }
        if (amount.compareTo(remainingAmount()) > 0) {
            throw new IllegalArgumentException("Consumed amount cannot exceed remaining hold amount");
        }
        this.consumedAmount = this.consumedAmount.add(amount);
    }

    public BigDecimal releaseRemaining() {
        BigDecimal remaining = remainingAmount();
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            this.releasedAmount = this.releasedAmount.add(remaining);
        }
        return remaining;
    }

    public BigDecimal remainingAmount() {
        return totalAmount.subtract(consumedAmount).subtract(releasedAmount);
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public HoldType getHoldType() {
        return holdType;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getConsumedAmount() {
        return consumedAmount;
    }

    public BigDecimal getReleasedAmount() {
        return releasedAmount;
    }
}
