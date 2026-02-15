package com.wisehero.stocktrading.common.api;

/**
 * 표준 성공 응답 코드.
 * {@link ApiResponse}의 code/message 조합으로 사용한다.
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
