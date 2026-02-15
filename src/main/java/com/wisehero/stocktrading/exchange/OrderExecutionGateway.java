package com.wisehero.stocktrading.exchange;

import com.wisehero.stocktrading.exchange.dto.MatchResult;
import com.wisehero.stocktrading.order.domain.Order;

/**
 * 브로커 코어가 체결 엔진을 호출할 때 사용하는 추상화.
 * 추후 외부 연동으로 교체할 수 있도록 인터페이스로 분리했다.
 */
public interface OrderExecutionGateway {

    MatchResult match(Order order);

    void cancel(Order order);
}
