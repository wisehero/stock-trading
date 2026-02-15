package com.wisehero.stocktrading.common.exception;

import com.wisehero.stocktrading.common.api.ApiErrorCode;

/**
 * Domain/application exception that carries a stable API error code.
 */
public class ApiException extends RuntimeException {

    private final ApiErrorCode errorCode;

    public ApiException(ApiErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ApiException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
