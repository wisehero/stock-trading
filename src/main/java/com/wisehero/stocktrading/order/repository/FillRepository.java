package com.wisehero.stocktrading.order.repository;

import com.wisehero.stocktrading.order.domain.Fill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FillRepository extends JpaRepository<Fill, Long> {

    List<Fill> findByOrderIdOrderByIdAsc(Long orderId);
}
