package com.wisehero.stocktrading.exchange;

import com.wisehero.stocktrading.exchange.dto.MatchResult;
import com.wisehero.stocktrading.order.domain.Order;
import com.wisehero.stocktrading.order.domain.OrderSide;
import com.wisehero.stocktrading.order.domain.OrderType;
import com.wisehero.stocktrading.quote.domain.MockQuote;
import com.wisehero.stocktrading.quote.repository.MockQuoteRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 1차 릴리즈용 내부 모의 매칭 엔진.
 * mock_quotes의 가격/가용수량으로 체결 여부를 판단한다.
 */
@Component
public class MockExchangeEngine implements OrderExecutionGateway {

    private final MockQuoteRepository mockQuoteRepository;

    public MockExchangeEngine(MockQuoteRepository mockQuoteRepository) {
        this.mockQuoteRepository = mockQuoteRepository;
    }

    @Override
    @Transactional
    public MatchResult match(Order order) {
        Optional<MockQuote> quoteOpt = mockQuoteRepository.findById(order.getSymbol());
        if (quoteOpt.isEmpty()) {
            return MatchResult.noFill();
        }

        MockQuote quote = quoteOpt.get();
        if (!isPriceConditionMatched(order, quote.getPrice())) {
            return MatchResult.noFill();
        }

        BigDecimal filledQuantity = quote.consumeAvailableQuantity(order.getRemainingQuantity());
        if (filledQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return MatchResult.noFill();
        }

        mockQuoteRepository.save(quote);

        if (filledQuantity.compareTo(order.getRemainingQuantity()) == 0) {
            return MatchResult.fullFill(filledQuantity, quote.getPrice(), UUID.randomUUID().toString());
        }

        return MatchResult.partialFill(filledQuantity, quote.getPrice(), UUID.randomUUID().toString());
    }

    @Override
    public void cancel(Order order) {
        // 내부 모의 엔진에서는 취소 요청을 즉시 수락 처리한다.
    }

    private boolean isPriceConditionMatched(Order order, BigDecimal quotePrice) {
        if (order.getOrderType() == OrderType.MARKET) {
            return true;
        }

        if (order.getSide() == OrderSide.BUY) {
            return quotePrice.compareTo(order.getLimitPrice()) <= 0;
        }

        return quotePrice.compareTo(order.getLimitPrice()) >= 0;
    }
}
