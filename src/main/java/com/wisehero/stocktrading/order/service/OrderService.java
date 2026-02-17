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
import com.wisehero.stocktrading.order.api.dto.OrderAmendRequest;
import com.wisehero.stocktrading.order.api.dto.OrderCreateRequest;
import com.wisehero.stocktrading.order.api.dto.OrderResponse;
import com.wisehero.stocktrading.order.domain.Fill;
import com.wisehero.stocktrading.order.domain.HoldType;
import com.wisehero.stocktrading.order.domain.Order;
import com.wisehero.stocktrading.order.domain.OrderHold;
import com.wisehero.stocktrading.order.domain.OrderSide;
import com.wisehero.stocktrading.order.domain.OrderStatus;
import com.wisehero.stocktrading.order.domain.OrderTif;
import com.wisehero.stocktrading.order.domain.OrderType;
import com.wisehero.stocktrading.order.repository.FillRepository;
import com.wisehero.stocktrading.order.repository.OrderHoldRepository;
import com.wisehero.stocktrading.order.repository.OrderRepository;
import com.wisehero.stocktrading.quote.domain.MockQuote;
import com.wisehero.stocktrading.quote.repository.MockQuoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 브로커 코어의 핵심 서비스.
 * 주문 생성/취소/정정/조회, 선점, 체결 반영, 잔량 해제를 한 곳에서 조율한다.
 */
@Service
public class OrderService {

    private static final List<OrderStatus> REMATCHABLE_STATUSES = List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 4;

    private final OrderRepository orderRepository;
    private final OrderHoldRepository orderHoldRepository;
    private final FillRepository fillRepository;
    private final MockQuoteRepository mockQuoteRepository;
    private final CashBalanceRepository cashBalanceRepository;
    private final PositionRepository positionRepository;
    private final OrderExecutionGateway orderExecutionGateway;
    private final BigDecimal feeRate;

    public OrderService(
            OrderRepository orderRepository,
            OrderHoldRepository orderHoldRepository,
            FillRepository fillRepository,
            MockQuoteRepository mockQuoteRepository,
            CashBalanceRepository cashBalanceRepository,
            PositionRepository positionRepository,
            OrderExecutionGateway orderExecutionGateway,
            @Value("${trading.fee-rate:0.00015}") BigDecimal feeRate
    ) {
        this.orderRepository = orderRepository;
        this.orderHoldRepository = orderHoldRepository;
        this.fillRepository = fillRepository;
        this.mockQuoteRepository = mockQuoteRepository;
        this.cashBalanceRepository = cashBalanceRepository;
        this.positionRepository = positionRepository;
        this.orderExecutionGateway = orderExecutionGateway;
        this.feeRate = feeRate == null || feeRate.compareTo(ZERO) < 0 ? ZERO : feeRate;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        String symbol = normalizeSymbol(request.symbol());
        OrderTif tif = resolveTif(request.orderType(), request.tif());
        validateCreateRequest(request, tif);

        // 멱등키가 같으면 기존 주문을 그대로 반환해 중복 주문 생성을 막는다.
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
                tif,
                request.limitPrice(),
                request.quantity()
        );
        orderRepository.save(order);

        OrderHold orderHold = reserveForOrder(order);
        orderHoldRepository.save(orderHold);

        // 주문 저장 직후 즉시 모의체결을 시도한다.
        order.markAccepted();
        applyMatch(order, orderHold);
        postProcessByTif(order, orderHold);

        orderRepository.save(order);
        orderHoldRepository.save(orderHold);

        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse amendOrder(Long orderId, OrderAmendRequest request) {
        Order order = getOrderOrThrow(orderId, request.accountId());
        validateAmendRequest(order, request);

        OrderHold hold = getOrderHoldOrThrow(orderId);

        BigDecimal amendedLimitPrice = request.amendedLimitPrice() == null
                ? order.getLimitPrice()
                : request.amendedLimitPrice();
        BigDecimal amendedRemainingQuantity = request.amendedRemainingQuantity() == null
                ? order.getRemainingQuantity()
                : request.amendedRemainingQuantity();

        if (isSameAmount(order.getLimitPrice(), amendedLimitPrice)
                && isSameAmount(order.getRemainingQuantity(), amendedRemainingQuantity)) {
            throw new ApiException(ApiErrorCode.ORDER_AMEND_NO_CHANGE);
        }

        adjustHoldForAmend(order, hold, amendedLimitPrice, amendedRemainingQuantity);
        order.amendLimitOrder(amendedLimitPrice, amendedRemainingQuantity);

        applyMatch(order, hold);
        postProcessByTif(order, hold);

        orderRepository.save(order);
        orderHoldRepository.save(hold);

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

            // 시세 갱신 시점에 DAY + NEW/PARTIALLY_FILLED 주문만 다시 체결 시도한다.
            if (order.getTif() != OrderTif.DAY) {
                continue;
            }

            OrderHold hold = getOrderHoldOrThrow(order.getId());
            applyMatch(order, hold);
            postProcessByTif(order, hold);

            orderRepository.save(order);
            orderHoldRepository.save(hold);
        }
    }

    @Transactional
    public int expireDayOrders() {
        List<Order> dayOrders = orderRepository.findByTifAndStatusInOrderByCreatedAtAscIdAsc(
                OrderTif.DAY,
                REMATCHABLE_STATUSES
        );

        int expiredCount = 0;
        for (Order order : dayOrders) {
            if (!order.isOpen()) {
                continue;
            }

            OrderHold hold = getOrderHoldOrThrow(order.getId());
            order.markExpired();
            releaseRemainingHold(order, hold);

            orderRepository.save(order);
            orderHoldRepository.save(hold);
            expiredCount++;
        }

        return expiredCount;
    }

    private void applyMatch(Order order, OrderHold hold) {
        MatchResult matchResult = orderExecutionGateway.match(order);
        if (!matchResult.hasFill()) {
            return;
        }

        BigDecimal fillQuantity = matchResult.fillQuantity();
        BigDecimal fillPrice = matchResult.fillPrice();
        BigDecimal notional = toMoney(fillPrice.multiply(fillQuantity));
        BigDecimal feeAmount = calculateFee(notional);
        BigDecimal taxAmount = ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        order.applyFill(fillQuantity);

        Fill fill = Fill.create(
                matchResult.executionId(),
                order.getId(),
                fillPrice,
                fillQuantity,
                feeAmount,
                taxAmount,
                Instant.now()
        );
        fillRepository.save(fill);

        if (order.getSide() == OrderSide.BUY) {
            applyBuyFill(order, hold, fillQuantity, fillPrice, notional, feeAmount);
        } else {
            applySellFill(order, hold, fillQuantity, notional, feeAmount);
        }

        // 완전체결되면 남아있는 선점분을 즉시 해제한다.
        if (!order.isOpen()) {
            releaseRemainingHold(order, hold);
        }
    }

    private void applyBuyFill(
            Order order,
            OrderHold hold,
            BigDecimal fillQuantity,
            BigDecimal fillPrice,
            BigDecimal notional,
            BigDecimal feeAmount
    ) {
        BigDecimal settlementAmount = notional.add(feeAmount);

        hold.consume(settlementAmount);

        CashBalance cashBalance = getCashBalanceOrThrow(order.getAccountId());
        cashBalance.consumeHeld(settlementAmount);
        cashBalanceRepository.save(cashBalance);

        PositionId positionId = new PositionId(order.getAccountId(), order.getSymbol());
        Position position = positionRepository.findById(positionId)
                .orElseGet(() -> Position.create(order.getAccountId(), order.getSymbol(), ZERO, ZERO));
        position.addBoughtQuantity(fillQuantity, fillPrice);
        positionRepository.save(position);
    }

    private void applySellFill(
            Order order,
            OrderHold hold,
            BigDecimal fillQuantity,
            BigDecimal notional,
            BigDecimal feeAmount
    ) {
        hold.consume(fillQuantity);

        Position position = getPositionOrThrow(order.getAccountId(), order.getSymbol());
        position.consumeHeld(fillQuantity);
        positionRepository.save(position);

        BigDecimal settlementAmount = notional.subtract(feeAmount);
        if (settlementAmount.compareTo(ZERO) > 0) {
            CashBalance cashBalance = getCashBalanceOrThrow(order.getAccountId());
            cashBalance.addAvailable(settlementAmount);
            cashBalanceRepository.save(cashBalance);
        }
    }

    private OrderHold reserveForOrder(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            return reserveCashForBuyOrder(order);
        }

        return reserveQuantityForSellOrder(order);
    }

    private OrderHold reserveCashForBuyOrder(Order order) {
        BigDecimal reserveAmount = calculateReserveAmount(resolveReservePrice(order), order.getQuantity());

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

    private void adjustHoldForAmend(
            Order order,
            OrderHold hold,
            BigDecimal amendedLimitPrice,
            BigDecimal amendedRemainingQuantity
    ) {
        if (order.getSide() == OrderSide.SELL) {
            adjustSellHoldForAmend(order, hold, amendedRemainingQuantity);
            return;
        }

        adjustBuyHoldForAmend(order, hold, amendedLimitPrice, amendedRemainingQuantity);
    }

    private void adjustSellHoldForAmend(Order order, OrderHold hold, BigDecimal amendedRemainingQuantity) {
        BigDecimal currentRemainingHold = hold.remainingAmount();
        if (amendedRemainingQuantity.compareTo(currentRemainingHold) > 0) {
            throw new ApiException(ApiErrorCode.ORDER_AMEND_INVALID_QUANTITY);
        }

        BigDecimal releaseQuantity = currentRemainingHold.subtract(amendedRemainingQuantity);
        if (releaseQuantity.compareTo(ZERO) <= 0) {
            return;
        }

        hold.release(releaseQuantity);

        Position position = getPositionOrThrow(order.getAccountId(), order.getSymbol());
        position.releaseHeld(releaseQuantity);
        positionRepository.save(position);
    }

    private void adjustBuyHoldForAmend(
            Order order,
            OrderHold hold,
            BigDecimal amendedLimitPrice,
            BigDecimal amendedRemainingQuantity
    ) {
        BigDecimal currentRemainingHold = hold.remainingAmount();
        BigDecimal targetRemainingHold = calculateReserveAmount(amendedLimitPrice, amendedRemainingQuantity);
        int compared = targetRemainingHold.compareTo(currentRemainingHold);

        if (compared == 0) {
            return;
        }

        CashBalance cashBalance = getCashBalanceOrThrow(order.getAccountId());
        if (compared > 0) {
            BigDecimal additionalHold = targetRemainingHold.subtract(currentRemainingHold);
            try {
                cashBalance.hold(additionalHold);
            } catch (IllegalStateException exception) {
                throw new ApiException(ApiErrorCode.ACCOUNT_INSUFFICIENT_CASH);
            }
            hold.increaseTotal(additionalHold);
            cashBalanceRepository.save(cashBalance);
            return;
        }

        BigDecimal releaseAmount = currentRemainingHold.subtract(targetRemainingHold);
        hold.release(releaseAmount);
        cashBalance.releaseHeld(releaseAmount);
        cashBalanceRepository.save(cashBalance);
    }

    private void postProcessByTif(Order order, OrderHold hold) {
        if (!order.isOpen()) {
            return;
        }

        if (order.getTif() == OrderTif.DAY) {
            return;
        }

        order.markCanceled();
        releaseRemainingHold(order, hold);
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

        // 시장가 주문은 현재 고정 시세를 기준으로 선점 금액을 계산한다.
        MockQuote quote = mockQuoteRepository.findById(order.getSymbol())
                .orElseThrow(() -> new ApiException(ApiErrorCode.QUOTE_NOT_FOUND));
        return quote.getPrice();
    }

    private BigDecimal calculateReserveAmount(BigDecimal reservePrice, BigDecimal quantity) {
        BigDecimal reserveNotional = toMoney(reservePrice.multiply(quantity));
        BigDecimal reserveFee = calculateFee(reserveNotional);
        return reserveNotional.add(reserveFee);
    }

    private BigDecimal calculateFee(BigDecimal notional) {
        if (feeRate.compareTo(ZERO) <= 0 || notional.compareTo(ZERO) <= 0) {
            return ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return toMoney(notional.multiply(feeRate));
    }

    private BigDecimal toMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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

    private void validateCreateRequest(OrderCreateRequest request, OrderTif tif) {
        if (request.quantity().compareTo(ZERO) <= 0) {
            throw new ApiException(ApiErrorCode.ORDER_INVALID_QUANTITY);
        }
        if (!isWholeShareQuantity(request.quantity())) {
            throw new ApiException(ApiErrorCode.ORDER_INVALID_QUANTITY_UNIT);
        }

        if (request.orderType() == OrderType.LIMIT && request.limitPrice() == null) {
            throw new ApiException(ApiErrorCode.ORDER_LIMIT_PRICE_REQUIRED);
        }

        if (request.orderType() == OrderType.MARKET && request.limitPrice() != null) {
            throw new ApiException(ApiErrorCode.ORDER_LIMIT_PRICE_NOT_ALLOWED);
        }

        if (request.orderType() == OrderType.MARKET && tif != OrderTif.IOC) {
            throw new ApiException(ApiErrorCode.ORDER_INVALID_TIF);
        }
    }

    private void validateAmendRequest(Order order, OrderAmendRequest request) {
        if (!order.isOpen()) {
            throw new ApiException(ApiErrorCode.ORDER_INVALID_STATUS);
        }
        if (order.getOrderType() != OrderType.LIMIT) {
            throw new ApiException(ApiErrorCode.ORDER_AMEND_NOT_ALLOWED);
        }
        if (request.amendedLimitPrice() == null && request.amendedRemainingQuantity() == null) {
            throw new ApiException(ApiErrorCode.ORDER_AMEND_INVALID_REQUEST);
        }

        if (request.amendedLimitPrice() != null && request.amendedLimitPrice().compareTo(ZERO) <= 0) {
            throw new ApiException(ApiErrorCode.ORDER_AMEND_INVALID_REQUEST);
        }

        if (request.amendedRemainingQuantity() == null) {
            return;
        }

        if (request.amendedRemainingQuantity().compareTo(ZERO) <= 0) {
            throw new ApiException(ApiErrorCode.ORDER_AMEND_INVALID_QUANTITY);
        }
        if (!isWholeShareQuantity(request.amendedRemainingQuantity())) {
            throw new ApiException(ApiErrorCode.ORDER_INVALID_QUANTITY_UNIT);
        }
        if (request.amendedRemainingQuantity().compareTo(order.getRemainingQuantity()) > 0) {
            throw new ApiException(ApiErrorCode.ORDER_AMEND_INVALID_QUANTITY);
        }
    }

    private OrderTif resolveTif(OrderType orderType, OrderTif requestedTif) {
        if (orderType == OrderType.MARKET) {
            if (requestedTif == null) {
                return OrderTif.IOC;
            }
            if (requestedTif != OrderTif.IOC) {
                throw new ApiException(ApiErrorCode.ORDER_INVALID_TIF);
            }
            return OrderTif.IOC;
        }

        return requestedTif == null ? OrderTif.DAY : requestedTif;
    }

    private boolean isWholeShareQuantity(BigDecimal quantity) {
        return quantity.stripTrailingZeros().scale() <= 0;
    }

    private boolean isSameAmount(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) == 0;
    }

    private String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase();
    }

    private OrderResponse toOrderResponse(Order order) {
        List<Fill> fills = fillRepository.findByOrderIdOrderByIdAsc(order.getId());
        return OrderResponse.from(order, fills);
    }
}
