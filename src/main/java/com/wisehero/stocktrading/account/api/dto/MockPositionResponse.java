package com.wisehero.stocktrading.account.api.dto;

import com.wisehero.stocktrading.account.domain.Position;
import java.math.BigDecimal;

/**
 * 모의 포지션 응답 DTO.
 */
public record MockPositionResponse(
        Long accountId,
        String symbol,
        BigDecimal availableQuantity,
        BigDecimal heldQuantity,
        BigDecimal averagePrice
) {

    public static MockPositionResponse from(Position position) {
        return new MockPositionResponse(
                position.getId().getAccountId(),
                position.getId().getSymbol(),
                position.getAvailableQuantity(),
                position.getHeldQuantity(),
                position.getAveragePrice()
        );
    }
}
