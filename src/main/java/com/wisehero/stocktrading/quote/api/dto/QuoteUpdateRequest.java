package com.wisehero.stocktrading.quote.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 모의 시세 등록/갱신 요청 DTO.
 */
public record QuoteUpdateRequest(
        @NotNull @DecimalMin("0.0001") @Digits(integer = 19, fraction = 4) BigDecimal price,
        @NotNull @DecimalMin("0.0000") @Digits(integer = 19, fraction = 4) BigDecimal availableQuantity
) {
}
