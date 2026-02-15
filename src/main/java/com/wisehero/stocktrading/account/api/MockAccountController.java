package com.wisehero.stocktrading.account.api;

import com.wisehero.stocktrading.account.api.dto.MockCashResponse;
import com.wisehero.stocktrading.account.api.dto.MockCashUpdateRequest;
import com.wisehero.stocktrading.account.api.dto.MockPositionResponse;
import com.wisehero.stocktrading.account.api.dto.MockPositionUpdateRequest;
import com.wisehero.stocktrading.account.domain.CashBalance;
import com.wisehero.stocktrading.account.domain.Position;
import com.wisehero.stocktrading.account.service.MockAccountService;
import com.wisehero.stocktrading.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발/테스트 시 모의 계좌와 포지션을 세팅하기 위한 API.
 */
@RestController
@Validated
@RequestMapping("/api/v1/mock/accounts")
public class MockAccountController {

    private final MockAccountService mockAccountService;

    public MockAccountController(MockAccountService mockAccountService) {
        this.mockAccountService = mockAccountService;
    }

    @PutMapping("/{accountId}/cash")
    public ApiResponse<MockCashResponse> upsertCash(
            @PathVariable @NotNull @Positive Long accountId,
            @Valid @RequestBody MockCashUpdateRequest request
    ) {
        CashBalance saved = mockAccountService.upsertCash(accountId, request);
        return ApiResponse.ok(MockCashResponse.from(saved));
    }

    @PutMapping("/{accountId}/positions/{symbol}")
    public ApiResponse<MockPositionResponse> upsertPosition(
            @PathVariable @NotNull @Positive Long accountId,
            @PathVariable String symbol,
            @Valid @RequestBody MockPositionUpdateRequest request
    ) {
        Position saved = mockAccountService.upsertPosition(accountId, symbol, request);
        return ApiResponse.ok(MockPositionResponse.from(saved));
    }
}
