package com.wisehero.stocktrading.quote.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Mock quote upsert request.
 */
public record QuoteUpdateRequest(
        @NotNull @DecimalMin("0.0001") @Digits(integer = 19, fraction = 4) BigDecimal price,
        @NotNull @DecimalMin("0.0000") @Digits(integer = 19, fraction = 4) BigDecimal availableQuantity
) {
}
