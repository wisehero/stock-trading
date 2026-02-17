package com.wisehero.stocktrading.order.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 주문 정정 요청 DTO.
 * 수량은 잔량 감소만 허용하며, 지정가 주문만 정정할 수 있다.
 */
public record OrderAmendRequest(
        @NotNull @Positive Long accountId,
        @DecimalMin("0.0001") @Digits(integer = 19, fraction = 4) BigDecimal amendedRemainingQuantity,
        @DecimalMin("0.0001") @Digits(integer = 19, fraction = 4) BigDecimal amendedLimitPrice
) {
}
