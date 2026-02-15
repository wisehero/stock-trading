package com.wisehero.stocktrading.order.api.dto;

import com.wisehero.stocktrading.order.domain.OrderSide;
import com.wisehero.stocktrading.order.domain.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Order create request for general orders.
 */
public record OrderCreateRequest(
        @NotNull @Positive Long accountId,
        @NotBlank String idempotencyKey,
        @NotBlank String symbol,
        @NotNull OrderSide side,
        @NotNull OrderType orderType,
        @NotNull @DecimalMin("0.0001") @Digits(integer = 19, fraction = 4) BigDecimal quantity,
        @DecimalMin("0.0001") @Digits(integer = 19, fraction = 4) BigDecimal limitPrice
) {
}
