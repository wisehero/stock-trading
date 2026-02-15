package com.wisehero.stocktrading.order.api.dto;

import com.wisehero.stocktrading.order.domain.Fill;
import com.wisehero.stocktrading.order.domain.Order;
import com.wisehero.stocktrading.order.domain.OrderSide;
import com.wisehero.stocktrading.order.domain.OrderStatus;
import com.wisehero.stocktrading.order.domain.OrderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Order detail response used by create/get/cancel APIs.
 */
public record OrderResponse(
        Long orderId,
        Long accountId,
        String symbol,
        OrderSide side,
        OrderType orderType,
        BigDecimal limitPrice,
        BigDecimal quantity,
        BigDecimal filledQuantity,
        BigDecimal remainingQuantity,
        OrderStatus status,
        String rejectReason,
        Instant createdAt,
        Instant updatedAt,
        List<FillResponse> fills
) {

    public static OrderResponse from(Order order, List<Fill> fills) {
        List<FillResponse> fillResponses = fills.stream()
                .map(FillResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getAccountId(),
                order.getSymbol(),
                order.getSide(),
                order.getOrderType(),
                order.getLimitPrice(),
                order.getQuantity(),
                order.getFilledQuantity(),
                order.getRemainingQuantity(),
                order.getStatus(),
                order.getRejectReason(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                fillResponses
        );
    }
}
