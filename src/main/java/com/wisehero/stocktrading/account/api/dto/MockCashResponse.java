package com.wisehero.stocktrading.account.api.dto;

import com.wisehero.stocktrading.account.domain.CashBalance;
import java.math.BigDecimal;

/**
 * 모의 계좌 현금 응답 DTO.
 */
public record MockCashResponse(
        Long accountId,
        BigDecimal availableCash,
        BigDecimal heldCash
) {

    public static MockCashResponse from(CashBalance cashBalance) {
        return new MockCashResponse(
                cashBalance.getAccountId(),
                cashBalance.getAvailableCash(),
                cashBalance.getHeldCash()
        );
    }
}
