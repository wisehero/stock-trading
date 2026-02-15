package com.wisehero.stocktrading.order.repository;

import com.wisehero.stocktrading.order.domain.Order;
import com.wisehero.stocktrading.order.domain.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByAccountIdAndIdempotencyKey(Long accountId, String idempotencyKey);

    Optional<Order> findByIdAndAccountId(Long id, Long accountId);

    List<Order> findBySymbolAndStatusInOrderByCreatedAtAscIdAsc(String symbol, Collection<OrderStatus> statuses);
}
