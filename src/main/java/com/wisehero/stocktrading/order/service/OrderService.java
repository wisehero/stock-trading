package com.wisehero.stocktrading.order.service;

import com.wisehero.stocktrading.account.domain.CashBalance;
import com.wisehero.stocktrading.account.domain.Position;
import com.wisehero.stocktrading.account.domain.PositionId;
import com.wisehero.stocktrading.account.repository.CashBalanceRepository;
import com.wisehero.stocktrading.account.repository.PositionRepository;
import com.wisehero.stocktrading.common.api.ApiErrorCode;
import com.wisehero.stocktrading.common.exception.ApiException;
import com.wisehero.stocktrading.exchange.OrderExecutionGateway;
import com.wisehero.stocktrading.exchange.dto.MatchResult;
import com.wisehero.stocktrading.order.api.dto.OrderCreateRequest;
import com.wisehero.stocktrading.order.api.dto.OrderResponse;
import com.wisehero.stocktrading.order.domain.Fill;
import com.wisehero.stocktrading.order.domain.HoldType;
import com.wisehero.stocktrading.order.domain.Order;
import com.wisehero.stocktrading.order.domain.OrderHold;
import com.wisehero.stocktrading.order.domain.OrderSide;
import com.wisehero.stocktrading.order.domain.OrderStatus;
import com.wisehero.stocktrading.order.domain.OrderType;
import com.wisehero.stocktrading.order.repository.FillRepository;
import com.wisehero.stocktrading.order.repository.OrderHoldRepository;
import com.wisehero.stocktrading.order.repository.OrderRepository;
import com.wisehero.stocktrading.quote.domain.MockQuote;
import com.wisehero.stocktrading.quote.repository.MockQuoteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Broker Core service that owns order lifecycle, holds, and post-fill updates.
 */
@Service
public class OrderService {

    private static final List<OrderStatus> REMATCHABLE_STATUSES = List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final OrderRepository orderRepository;
    private final OrderHoldRepository orderHoldRepository;
    private final FillRepository fillRepository;
    private final MockQuoteRepository mockQuoteRepository;
    private final CashBalanceRepository cashBalanceRepository;
    private final PositionRepository positionRepository;
    private final OrderExecutionGateway orderExecutionGateway;

    public OrderService(
            OrderRepository orderRepository,
            OrderHoldRepository orderHoldRepository,
            FillRepository fillRepository,
            MockQuoteRepository mockQuoteRepository,
            CashBalanceRepository cashBalanceRepository,
            PositionRepository positionRepository,
            OrderExecutionGateway orderExecutionGateway
    ) {
        this.orderRepository = orderRepository;
        this.orderHoldRepository = orderHoldRepository;
        this.fillRepository = fillRepository;
        this.mockQuoteRepository = mockQuoteRepository;
        this.cashBalanceRepository = cashBalanceRepository;
        this.positionRepository = positionRepository;
        this.orderExecutionGateway = orderExecutionGateway;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        String symbol = normalizeSymbol(request.symbol());
        validateCreateRequest(request);

        Order existingOrder = orderRepository.findByAccountIdAndIdempotencyKey(request.accountId(), request.idempotencyKey())
                .orElse(null);
        if (existingOrder != null) {
            return toOrderResponse(existingOrder);
        }

        Order order = Order.newPending(
                request.accountId(),
                request.idempotencyKey(),
                symbol,
                request.side(),
                request.orderType(),
                request.limitPrice(),
                request.quantity()
        );
        orderRepository.save(order);

        OrderHold orderHold = reserveForOrder(order);
        orderHoldRepository.save(orderHold);

        order.markAccepted();
        applyMatch(order, orderHold);

        orderRepository.save(order);
        orderHoldRepository.save(orderHold);

        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long accountId) {
        Order order = getOrderOrThrow(orderId, accountId);
        if (!order.getStatus().isCancelable()) {
            throw new ApiException(ApiErrorCode.ORDER_INVALID_STATUS);
        }

        OrderHold hold = getOrderHoldOrThrow(orderId);

        order.markCancelPending();
        orderExecutionGateway.cancel(order);
        order.markCanceled();
        releaseRemainingHold(order, hold);

        orderRepository.save(order);
        orderHoldRepository.save(hold);

        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, Long accountId) {
        Order order = getOrderOrThrow(orderId, accountId);
        return toOrderResponse(order);
    }

    @Transactional
    public void rematchOpenOrdersForSymbol(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        List<Order> openOrders = orderRepository.findBySymbolAndStatusInOrderByCreatedAtAscIdAsc(
                normalizedSymbol,
                REMATCHABLE_STATUSES
        );

        for (Order order : openOrders) {
            if (!order.isOpen()) {
                continue;
            }

            OrderHold hold = getOrderHoldOrThrow(order.getId());
            applyMatch(order, hold);

            orderRepository.save(order);
            orderHoldRepository.save(hold);
        }
    }

    private void applyMatch(Order order, OrderHold hold) {
        MatchResult matchResult = orderExecutionGateway.match(order);
        if (!matchResult.hasFill()) {
            return;
        }

        BigDecimal fillQuantity = matchResult.fillQuantity();
        BigDecimal fillPrice = matchResult.fillPrice();

        order.applyFill(fillQuantity);

        Fill fill = Fill.create(
                matchResult.executionId(),
                order.getId(),
                fillPrice,
                fillQuantity,
                ZERO,
                ZERO,
                Instant.now()
        );
        fillRepository.save(fill);

        if (order.getSide() == OrderSide.BUY) {
            applyBuyFill(order, hold, fillQuantity, fillPrice);
        } else {
            applySellFill(order, hold, fillQuantity, fillPrice);
        }

        if (!order.isOpen()) {
            releaseRemainingHold(order, hold);
        }
    }

    private void applyBuyFill(Order order, OrderHold hold, BigDecimal fillQuantity, BigDecimal fillPrice) {
        BigDecimal notional = fillPrice.multiply(fillQuantity);

        hold.consume(notional);

        CashBalance cashBalance = getCashBalanceOrThrow(order.getAccountId());
        cashBalance.consumeHeld(notional);
        cashBalanceRepository.save(cashBalance);

        PositionId positionId = new PositionId(order.getAccountId(), order.getSymbol());
        Position position = positionRepository.findById(positionId)
                .orElseGet(() -> Position.create(order.getAccountId(), order.getSymbol(), ZERO, ZERO));
        position.addBoughtQuantity(fillQuantity, fillPrice);
        positionRepository.save(position);
    }

    private void applySellFill(Order order, OrderHold hold, BigDecimal fillQuantity, BigDecimal fillPrice) {
        BigDecimal notional = fillPrice.multiply(fillQuantity);

        hold.consume(fillQuantity);

        Position position = getPositionOrThrow(order.getAccountId(), order.getSymbol());
        position.consumeHeld(fillQuantity);
        positionRepository.save(position);

        CashBalance cashBalance = getCashBalanceOrThrow(order.getAccountId());
        cashBalance.addAvailable(notional);
        cashBalanceRepository.save(cashBalance);
    }

    private OrderHold reserveForOrder(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            return reserveCashForBuyOrder(order);
        }

        return reserveQuantityForSellOrder(order);
    }

    private OrderHold reserveCashForBuyOrder(Order order) {
        BigDecimal reservePrice = resolveReservePrice(order);
        BigDecimal reserveAmount = reservePrice.multiply(order.getQuantity());

        CashBalance cashBalance = getCashBalanceOrThrow(order.getAccountId());
        try {
            cashBalance.hold(reserveAmount);
        } catch (IllegalStateException exception) {
            throw new ApiException(ApiErrorCode.ACCOUNT_INSUFFICIENT_CASH);
        }
        cashBalanceRepository.save(cashBalance);

        return OrderHold.create(order.getId(), order.getAccountId(), HoldType.CASH, reserveAmount);
    }

    private OrderHold reserveQuantityForSellOrder(Order order) {
        Position position = getPositionOrThrow(order.getAccountId(), order.getSymbol());
        try {
            position.hold(order.getQuantity());
        } catch (IllegalStateException exception) {
            throw new ApiException(ApiErrorCode.ACCOUNT_INSUFFICIENT_QUANTITY);
        }
        positionRepository.save(position);

        return OrderHold.create(order.getId(), order.getAccountId(), HoldType.QUANTITY, order.getQuantity());
    }

    private void releaseRemainingHold(Order order, OrderHold hold) {
        BigDecimal releaseAmount = hold.releaseRemaining();
        if (releaseAmount.compareTo(ZERO) <= 0) {
            return;
        }

        if (hold.getHoldType() == HoldType.CASH) {
            CashBalance cashBalance = getCashBalanceOrThrow(order.getAccountId());
            cashBalance.releaseHeld(releaseAmount);
            cashBalanceRepository.save(cashBalance);
            return;
        }

        Position position = getPositionOrThrow(order.getAccountId(), order.getSymbol());
        position.releaseHeld(releaseAmount);
        positionRepository.save(position);
    }

    private BigDecimal resolveReservePrice(Order order) {
        if (order.getOrderType() == OrderType.LIMIT) {
            return order.getLimitPrice();
        }

        MockQuote quote = mockQuoteRepository.findById(order.getSymbol())
                .orElseThrow(() -> new ApiException(ApiErrorCode.QUOTE_NOT_FOUND));
        return quote.getPrice();
    }

    private Order getOrderOrThrow(Long orderId, Long accountId) {
        return orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.ORDER_NOT_FOUND));
    }

    private OrderHold getOrderHoldOrThrow(Long orderId) {
        return orderHoldRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.ORDER_INVALID_STATUS, "주문 선점 정보를 찾을 수 없습니다."));
    }

    private CashBalance getCashBalanceOrThrow(Long accountId) {
        return cashBalanceRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.ACCOUNT_NOT_FOUND));
    }

    private Position getPositionOrThrow(Long accountId, String symbol) {
        return positionRepository.findById(new PositionId(accountId, symbol))
                .orElseThrow(() -> new ApiException(ApiErrorCode.ACCOUNT_POSITION_NOT_FOUND));
    }

    private void validateCreateRequest(OrderCreateRequest request) {
        if (request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiErrorCode.ORDER_INVALID_QUANTITY);
        }

        if (request.orderType() == OrderType.LIMIT && request.limitPrice() == null) {
            throw new ApiException(ApiErrorCode.ORDER_LIMIT_PRICE_REQUIRED);
        }

        if (request.orderType() == OrderType.MARKET && request.limitPrice() != null) {
            throw new ApiException(ApiErrorCode.ORDER_LIMIT_PRICE_NOT_ALLOWED);
        }
    }

    private String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase();
    }

    private OrderResponse toOrderResponse(Order order) {
        List<Fill> fills = fillRepository.findByOrderIdOrderByIdAsc(order.getId());
        return OrderResponse.from(order, fills);
    }
}
