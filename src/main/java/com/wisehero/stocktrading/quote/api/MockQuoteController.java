package com.wisehero.stocktrading.quote.api;

import com.wisehero.stocktrading.common.api.ApiResponse;
import com.wisehero.stocktrading.quote.api.dto.QuoteResponse;
import com.wisehero.stocktrading.quote.api.dto.QuoteUpdateRequest;
import com.wisehero.stocktrading.quote.domain.MockQuote;
import com.wisehero.stocktrading.quote.service.MockQuoteService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock quote APIs used for deterministic matching tests.
 */
@RestController
@Validated
@RequestMapping("/api/v1/mock/quotes")
public class MockQuoteController {

    private final MockQuoteService mockQuoteService;

    public MockQuoteController(MockQuoteService mockQuoteService) {
        this.mockQuoteService = mockQuoteService;
    }

    @PutMapping("/{symbol}")
    public ApiResponse<QuoteResponse> upsertQuote(
            @PathVariable String symbol,
            @Valid @RequestBody QuoteUpdateRequest request
    ) {
        MockQuote savedQuote = mockQuoteService.upsertQuote(symbol, request);
        return ApiResponse.ok(QuoteResponse.from(savedQuote));
    }
}
