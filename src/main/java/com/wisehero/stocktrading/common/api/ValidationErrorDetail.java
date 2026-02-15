package com.wisehero.stocktrading.common.api;

/**
 * Field-level validation error detail.
 */
public record ValidationErrorDetail(String field, String reason) {
}
