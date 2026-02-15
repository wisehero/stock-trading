package com.wisehero.stocktrading.common.api;

import java.util.List;

/**
 * Validation error payload container for {@link ApiResponse}.
 */
public record ValidationErrorData(List<ValidationErrorDetail> errors) {
}
