package com.wisehero.stocktrading.account.service;

import com.wisehero.stocktrading.account.api.dto.MockCashUpdateRequest;
import com.wisehero.stocktrading.account.api.dto.MockPositionUpdateRequest;
import com.wisehero.stocktrading.account.domain.CashBalance;
import com.wisehero.stocktrading.account.domain.Position;
import com.wisehero.stocktrading.account.domain.PositionId;
import com.wisehero.stocktrading.account.repository.CashBalanceRepository;
import com.wisehero.stocktrading.account.repository.PositionRepository;
import com.wisehero.stocktrading.common.api.ApiErrorCode;
import com.wisehero.stocktrading.common.exception.ApiException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트/개발용 모의 계좌 데이터를 설정하는 서비스.
 */
@Service
public class MockAccountService {

    private final CashBalanceRepository cashBalanceRepository;
    private final PositionRepository positionRepository;

    public MockAccountService(CashBalanceRepository cashBalanceRepository, PositionRepository positionRepository) {
        this.cashBalanceRepository = cashBalanceRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional
    public CashBalance upsertCash(Long accountId, MockCashUpdateRequest request) {
        CashBalance cashBalance = cashBalanceRepository.findById(accountId)
                .orElseGet(() -> CashBalance.create(accountId, request.availableCash()));
        cashBalance.updateForMock(request.availableCash());
        return cashBalanceRepository.save(cashBalance);
    }

    @Transactional
    public Position upsertPosition(Long accountId, String rawSymbol, MockPositionUpdateRequest request) {
        if (!isWholeShareQuantity(request.availableQuantity())) {
            throw new ApiException(ApiErrorCode.ORDER_INVALID_QUANTITY_UNIT);
        }

        String symbol = normalizeSymbol(rawSymbol);
        PositionId positionId = new PositionId(accountId, symbol);

        Position position = positionRepository.findById(positionId)
                .orElseGet(() -> Position.create(accountId, symbol, request.availableQuantity(), request.averagePrice()));
        position.updateForMock(request.availableQuantity(), request.averagePrice());
        return positionRepository.save(position);
    }

    private String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase();
    }

    private boolean isWholeShareQuantity(BigDecimal quantity) {
        return quantity.stripTrailingZeros().scale() <= 0;
    }
}
