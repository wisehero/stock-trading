package com.wisehero.stocktrading.order.api.dto;

import com.wisehero.stocktrading.order.domain.Fill;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fill response projection.
 */
public record FillResponse(
        String executionId,
        BigDecimal fillPrice,
        BigDecimal fillQuantity,
        BigDecimal feeAmount,
        BigDecimal taxAmount,
        Instant filledAt
) {

    public static FillResponse from(Fill fill) {
        return new FillResponse(
                fill.getExecutionId(),
                fill.getFillPrice(),
                fill.getFillQuantity(),
                fill.getFeeAmount(),
                fill.getTaxAmount(),
                fill.getFilledAt()
        );
    }
}
