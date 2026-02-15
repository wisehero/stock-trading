package com.wisehero.stocktrading.common.api;

import java.util.List;

/**
 * 유효성 검증 실패 시 내려가는 에러 데이터 묶음.
 */
public record ValidationErrorData(List<ValidationErrorDetail> errors) {
}
