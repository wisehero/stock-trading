package com.wisehero.stocktrading.account.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 모의 포지션 세팅 요청 DTO.
 */
public record MockPositionUpdateRequest(
        @NotNull @DecimalMin("0.0000") @Digits(integer = 19, fraction = 4) BigDecimal availableQuantity,
        @NotNull @DecimalMin("0.0000") @Digits(integer = 19, fraction = 4) BigDecimal averagePrice
) {
}
