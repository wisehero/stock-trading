package com.wisehero.stocktrading.common.api;

import java.util.List;

public record ValidationErrorData(List<ValidationErrorDetail> errors) {
}
