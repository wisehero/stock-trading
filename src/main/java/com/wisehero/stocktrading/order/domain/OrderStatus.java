package com.wisehero.stocktrading.order.domain;

/**
 * Order lifecycle states used by Broker Core.
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
