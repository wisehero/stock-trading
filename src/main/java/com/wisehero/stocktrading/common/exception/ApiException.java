package com.wisehero.stocktrading.common.exception;

import com.wisehero.stocktrading.common.api.ApiErrorCode;

/**
 * 안정적인 API 에러코드를 함께 전달하기 위한 애플리케이션 예외.
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
