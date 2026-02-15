package com.wisehero.stocktrading.common.api;

/**
 * Standard success codes used by {@link ApiResponse}.
 */
public enum ApiSuccessCode {
    OK("COMMON-200", "요청이 성공했습니다."),
    CREATED("COMMON-201", "리소스가 생성되었습니다."),
    NO_CONTENT("COMMON-204", "요청이 성공했습니다.");

    private final String code;
    private final String message;

    ApiSuccessCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
