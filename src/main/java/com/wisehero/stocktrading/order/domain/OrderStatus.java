package com.wisehero.stocktrading.order.domain;

/**
 * Broker Core에서 사용하는 주문 상태 전이 집합.
 */
public enum OrderStatus {
    PENDING_NEW,
    NEW,
    REJECTED,
    PARTIALLY_FILLED,
    FILLED,
    PENDING_CANCEL,
    CANCELED,
    EXPIRED;

    public boolean isOpen() {
        return this == NEW || this == PARTIALLY_FILLED;
    }

    public boolean isCancelable() {
        return isOpen();
    }
}
