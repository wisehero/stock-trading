package com.wisehero.stocktrading.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/**
 * 표준 API 응답 래퍼.
 * 응답 형식은 {@code {code, message, data}}로 고정한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String code, String message, T data) {

    /** HTTP 200 의미의 성공 응답을 생성한다. */
    public static <T> ApiResponse<T> ok(T data) {
        return success(ApiSuccessCode.OK, data);
    }

    /** 페이로드 없는 성공 응답을 생성한다. */
    public static ApiResponse<Void> ok() {
        return success(ApiSuccessCode.OK, null);
    }

    /** HTTP 201 의미의 성공 응답을 생성한다. */
    public static <T> ApiResponse<T> created(T data) {
        return success(ApiSuccessCode.CREATED, data);
    }

    /** HTTP 204 의미의 성공 응답을 생성한다. */
    public static ApiResponse<Void> noContent() {
        return success(ApiSuccessCode.NO_CONTENT, null);
    }

    /** {@link ApiErrorCode}의 기본 메시지를 사용한 에러 응답을 생성한다. */
    public static ApiResponse<Void> error(ApiErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null);
    }

    /** 상세 에러 데이터(data)를 포함한 에러 응답을 생성한다. */
    public static <T> ApiResponse<T> error(ApiErrorCode errorCode, T data) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), data);
    }

    /** 기본 메시지를 덮어쓴 에러 응답을 생성한다. */
    public static ApiResponse<Void> error(ApiErrorCode errorCode, String message) {
        String resolvedMessage = Objects.requireNonNullElse(message, errorCode.message());
        return new ApiResponse<>(errorCode.code(), resolvedMessage, null);
    }

    private static <T> ApiResponse<T> success(ApiSuccessCode successCode, T data) {
        return new ApiResponse<>(successCode.code(), successCode.message(), data);
    }
}
