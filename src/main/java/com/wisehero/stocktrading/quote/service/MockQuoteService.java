package com.wisehero.stocktrading.quote.service;

import com.wisehero.stocktrading.common.api.ApiErrorCode;
import com.wisehero.stocktrading.common.exception.ApiException;
import com.wisehero.stocktrading.order.service.OrderService;
import com.wisehero.stocktrading.quote.api.dto.QuoteUpdateRequest;
import com.wisehero.stocktrading.quote.domain.MockQuote;
import com.wisehero.stocktrading.quote.repository.MockQuoteRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모의 시세를 갱신하고, 갱신 직후 미체결 주문 재매칭을 트리거한다.
 */
@Service
public class MockQuoteService {

    private final MockQuoteRepository mockQuoteRepository;
    private final OrderService orderService;

    public MockQuoteService(MockQuoteRepository mockQuoteRepository, OrderService orderService) {
        this.mockQuoteRepository = mockQuoteRepository;
        this.orderService = orderService;
    }

    @Transactional
    public MockQuote upsertQuote(String rawSymbol, QuoteUpdateRequest request) {
        validateQuoteRequest(request);
        String symbol = normalizeSymbol(rawSymbol);

        MockQuote quote = mockQuoteRepository.findById(symbol)
                .orElseGet(() -> MockQuote.create(symbol, request.price(), request.availableQuantity()));
        quote.update(request.price(), request.availableQuantity());

        MockQuote savedQuote = mockQuoteRepository.save(quote);
        orderService.rematchOpenOrdersForSymbol(symbol);
        return savedQuote;
    }

    private void validateQuoteRequest(QuoteUpdateRequest request) {
        if (request.price().compareTo(BigDecimal.ZERO) <= 0 || request.availableQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ApiErrorCode.QUOTE_INVALID_VALUE);
        }
    }

    private String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase();
    }
}
