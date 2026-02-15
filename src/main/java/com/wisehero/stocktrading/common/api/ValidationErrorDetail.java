package com.wisehero.stocktrading.common.api;

/**
 * 필드 단위 유효성 검증 실패 상세 정보.
 */
public record ValidationErrorDetail(String field, String reason) {
}
