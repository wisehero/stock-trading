package com.wisehero.stocktrading.common.api;

import org.springframework.http.HttpStatus;

/**
 * 표준 에러 코드와 HTTP 상태값 매핑.
 * 컨트롤러/서비스에서는 이 코드를 기준으로 예외를 던진다.
 */
public enum ApiErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON-401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON-403", "접근 권한이 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-405", "허용되지 않은 HTTP 메서드입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-404", "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON-409", "요청이 현재 상태와 충돌합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다."),

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-404", "주문을 찾을 수 없습니다."),
    ORDER_LIMIT_PRICE_REQUIRED(HttpStatus.BAD_REQUEST, "ORDER-400", "지정가 주문은 가격이 필요합니다."),
    ORDER_LIMIT_PRICE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ORDER-401", "시장가 주문에는 가격을 지정할 수 없습니다."),
    ORDER_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER-402", "주문 수량은 0보다 커야 합니다."),
    ORDER_INVALID_QUANTITY_UNIT(HttpStatus.BAD_REQUEST, "ORDER-411", "주문 수량은 1주 단위여야 합니다."),
    ORDER_INVALID_TIF(HttpStatus.BAD_REQUEST, "ORDER-410", "주문 유형과 유효시간(TIF) 조합이 올바르지 않습니다."),
    ORDER_INVALID_STATUS(HttpStatus.CONFLICT, "ORDER-409", "현재 주문 상태에서는 요청을 처리할 수 없습니다."),
    ORDER_AMEND_NOT_ALLOWED(HttpStatus.CONFLICT, "ORDER-412", "현재 주문은 정정할 수 없습니다."),
    ORDER_AMEND_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "ORDER-413", "유효하지 않은 정정 요청입니다."),
    ORDER_AMEND_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER-414", "정정 수량은 현재 잔량 이하로만 줄일 수 있습니다."),
    ORDER_AMEND_NO_CHANGE(HttpStatus.BAD_REQUEST, "ORDER-415", "정정 대상 값이 기존 주문과 동일합니다."),

    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-404", "계좌를 찾을 수 없습니다."),
    ACCOUNT_POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-405", "보유 종목을 찾을 수 없습니다."),
    ACCOUNT_INSUFFICIENT_CASH(HttpStatus.CONFLICT, "ACCOUNT-409", "주문 가능 금액이 부족합니다."),
    ACCOUNT_INSUFFICIENT_QUANTITY(HttpStatus.CONFLICT, "ACCOUNT-410", "주문 가능 수량이 부족합니다."),

    QUOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "QUOTE-404", "시세 정보를 찾을 수 없습니다."),
    QUOTE_INVALID_VALUE(HttpStatus.BAD_REQUEST, "QUOTE-400", "유효하지 않은 시세 값입니다.");

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
