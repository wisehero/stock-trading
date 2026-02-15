package com.wisehero.stocktrading.account.repository;

import com.wisehero.stocktrading.account.domain.CashBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashBalanceRepository extends JpaRepository<CashBalance, Long> {
}
