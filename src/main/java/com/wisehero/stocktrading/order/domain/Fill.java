package com.wisehero.stocktrading.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Executed fill (trade) produced by Mock Exchange.
 */
@Entity
@Table(
        name = "fills",
        indexes = {
                @Index(name = "idx_fills_order_id", columnList = "order_id")
        }
)
public class Fill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, unique = true, length = 36)
    private String executionId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "fill_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal fillPrice;

    @Column(name = "fill_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal fillQuantity;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "filled_at", nullable = false)
    private Instant filledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Fill() {
    }

    public static Fill create(
            String executionId,
            Long orderId,
            BigDecimal fillPrice,
            BigDecimal fillQuantity,
            BigDecimal feeAmount,
            BigDecimal taxAmount,
            Instant filledAt
    ) {
        Fill fill = new Fill();
        fill.executionId = executionId;
        fill.orderId = orderId;
        fill.fillPrice = fillPrice;
        fill.fillQuantity = fillQuantity;
        fill.feeAmount = feeAmount;
        fill.taxAmount = taxAmount;
        fill.filledAt = filledAt;
        return fill;
    }

    public BigDecimal notional() {
        return fillPrice.multiply(fillQuantity);
    }

    public Long getId() {
        return id;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getFillPrice() {
        return fillPrice;
    }

    public BigDecimal getFillQuantity() {
        return fillQuantity;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public Instant getFilledAt() {
        return filledAt;
    }
}
