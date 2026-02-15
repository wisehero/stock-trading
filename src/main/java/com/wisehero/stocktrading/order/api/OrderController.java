package com.wisehero.stocktrading.order.api;

import com.wisehero.stocktrading.common.api.ApiResponse;
import com.wisehero.stocktrading.order.api.dto.OrderCancelRequest;
import com.wisehero.stocktrading.order.api.dto.OrderCreateRequest;
import com.wisehero.stocktrading.order.api.dto.OrderResponse;
import com.wisehero.stocktrading.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 일반주문(매수/매도) API 진입점.
 */
@RestController
@Validated
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.created(orderService.createOrder(request));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCancelRequest request
    ) {
        return ApiResponse.ok(orderService.cancelOrder(orderId, request.accountId()));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable Long orderId,
            @RequestParam("accountId") @NotNull @Positive Long accountId
    ) {
        return ApiResponse.ok(orderService.getOrder(orderId, accountId));
    }
}
