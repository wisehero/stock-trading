package com.wisehero.stocktrading.exchange;

import com.wisehero.stocktrading.exchange.dto.MatchResult;
import com.wisehero.stocktrading.order.domain.Order;

/**
 * Broker-side abstraction for exchange execution.
 */
public interface OrderExecutionGateway {

    MatchResult match(Order order);

    void cancel(Order order);
}
