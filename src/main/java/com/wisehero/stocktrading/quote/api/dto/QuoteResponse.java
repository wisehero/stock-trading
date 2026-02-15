package com.wisehero.stocktrading.quote.api.dto;

import com.wisehero.stocktrading.quote.domain.MockQuote;
import java.math.BigDecimal;

/**
 * 모의 시세 응답 DTO.
 */
public record QuoteResponse(
        String symbol,
        BigDecimal price,
        BigDecimal availableQuantity
) {

    public static QuoteResponse from(MockQuote quote) {
        return new QuoteResponse(quote.getSymbol(), quote.getPrice(), quote.getAvailableQuantity());
    }
}
