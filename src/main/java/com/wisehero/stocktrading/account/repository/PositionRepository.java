package com.wisehero.stocktrading.account.repository;

import com.wisehero.stocktrading.account.domain.Position;
import com.wisehero.stocktrading.account.domain.PositionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, PositionId> {
}
