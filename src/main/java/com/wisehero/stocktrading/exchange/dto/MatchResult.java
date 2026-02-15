package com.wisehero.stocktrading.exchange.dto;

import java.math.BigDecimal;

/**
 * Matching outcome for a single order evaluation.
 */
public record MatchResult(
        MatchType type,
        BigDecimal fillQuantity,
        BigDecimal fillPrice,
        String executionId
) {

    public static MatchResult noFill() {
        return new MatchResult(MatchType.NO_FILL, BigDecimal.ZERO, null, null);
    }

    public static MatchResult partialFill(BigDecimal fillQuantity, BigDecimal fillPrice, String executionId) {
        return new MatchResult(MatchType.PARTIAL_FILL, fillQuantity, fillPrice, executionId);
    }

    public static MatchResult fullFill(BigDecimal fillQuantity, BigDecimal fillPrice, String executionId) {
        return new MatchResult(MatchType.FULL_FILL, fillQuantity, fillPrice, executionId);
    }

    public boolean hasFill() {
        return type == MatchType.PARTIAL_FILL || type == MatchType.FULL_FILL;
    }
}
