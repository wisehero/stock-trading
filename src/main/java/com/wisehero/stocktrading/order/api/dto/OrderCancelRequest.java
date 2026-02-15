package com.wisehero.stocktrading.order.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 주문 취소 요청 DTO.
 * 계좌 본인 여부 검증을 위해 accountId를 함께 전달한다.
 */
public record OrderCancelRequest(
        @NotNull @Positive Long accountId
) {
}
