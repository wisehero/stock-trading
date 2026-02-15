package com.wisehero.stocktrading.common.api;

import org.springframework.http.HttpStatus;

/**
 * Standard error codes and mapped HTTP status values.
 */
public enum ApiErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON-401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON-403", "접근 권한이 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-405", "허용되지 않은 HTTP 메서드입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-404", "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON-409", "요청이 현재 상태와 충돌합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ApiErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
