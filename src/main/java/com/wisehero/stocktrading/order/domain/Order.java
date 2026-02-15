package com.wisehero.stocktrading.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Aggregate root for order lifecycle and state transitions.
 */
@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_orders_account_idempotency",
                        columnNames = {"account_id", "idempotency_key"}
                )
        },
        indexes = {
                @Index(name = "idx_orders_symbol_status_created", columnList = "symbol,status,created_at"),
                @Index(name = "idx_orders_account_id", columnList = "account_id")
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @Column(name = "limit_price", precision = 19, scale = 4)
    private BigDecimal limitPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "filled_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal filledQuantity;

    @Column(name = "remaining_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Order() {
    }

    public static Order newPending(
            Long accountId,
            String idempotencyKey,
            String symbol,
            OrderSide side,
            OrderType orderType,
            BigDecimal limitPrice,
            BigDecimal quantity
    ) {
        Order order = new Order();
        order.accountId = accountId;
        order.idempotencyKey = idempotencyKey;
        order.symbol = symbol;
        order.side = side;
        order.orderType = orderType;
        order.limitPrice = limitPrice;
        order.quantity = quantity;
        order.filledQuantity = BigDecimal.ZERO;
        order.remainingQuantity = quantity;
        order.status = OrderStatus.PENDING_NEW;
        return order;
    }

    public void markAccepted() {
        if (status != OrderStatus.PENDING_NEW) {
            throw new IllegalStateException("Order can be accepted only from PENDING_NEW");
        }
        this.status = OrderStatus.NEW;
        this.rejectReason = null;
    }

    public void reject(String reason) {
        if (status != OrderStatus.PENDING_NEW && status != OrderStatus.NEW) {
            throw new IllegalStateException("Order can be rejected only from pending/new state");
        }
        this.status = OrderStatus.REJECTED;
        this.rejectReason = Objects.requireNonNullElse(reason, "Order rejected");
    }

    public void applyFill(BigDecimal fillQuantity) {
        if (!status.isOpen()) {
            throw new IllegalStateException("Order can be filled only while open");
        }
        if (fillQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fill quantity must be positive");
        }
        if (fillQuantity.compareTo(remainingQuantity) > 0) {
            throw new IllegalArgumentException("Fill quantity cannot exceed remaining quantity");
        }

        this.filledQuantity = this.filledQuantity.add(fillQuantity);
        this.remainingQuantity = this.remainingQuantity.subtract(fillQuantity);

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            this.status = OrderStatus.FILLED;
            return;
        }

        this.status = OrderStatus.PARTIALLY_FILLED;
    }

    public void markCancelPending() {
        if (!status.isCancelable()) {
            throw new IllegalStateException("Order is not cancelable in current status");
        }
        this.status = OrderStatus.PENDING_CANCEL;
    }

    public void markCanceled() {
        if (status != OrderStatus.PENDING_CANCEL && status != OrderStatus.NEW && status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException("Order cannot be canceled in current status");
        }
        this.status = OrderStatus.CANCELED;
    }

    public boolean isOpen() {
        return status.isOpen();
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getFilledQuantity() {
        return filledQuantity;
    }

    public BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
