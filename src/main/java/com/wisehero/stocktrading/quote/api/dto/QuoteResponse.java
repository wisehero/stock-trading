package com.wisehero.stocktrading.quote.api.dto;

import com.wisehero.stocktrading.quote.domain.MockQuote;
import java.math.BigDecimal;

/**
 * Mock quote response.
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
