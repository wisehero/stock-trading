package com.wisehero.stocktrading.order.repository;

import com.wisehero.stocktrading.order.domain.OrderHold;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderHoldRepository extends JpaRepository<OrderHold, Long> {

    Optional<OrderHold> findByOrderId(Long orderId);
}
