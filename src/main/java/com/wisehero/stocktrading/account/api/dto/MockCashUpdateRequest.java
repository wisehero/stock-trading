package com.wisehero.stocktrading.account.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Mock account cash update request.
 */
public record MockCashUpdateRequest(
        @NotNull @DecimalMin("0.0000") @Digits(integer = 19, fraction = 4) BigDecimal availableCash
) {
}
