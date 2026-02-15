package com.wisehero.stocktrading.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return success(ApiSuccessCode.OK, data);
    }

    public static ApiResponse<Void> ok() {
        return success(ApiSuccessCode.OK, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return success(ApiSuccessCode.CREATED, data);
    }

    public static ApiResponse<Void> noContent() {
        return success(ApiSuccessCode.NO_CONTENT, null);
    }

    public static ApiResponse<Void> error(ApiErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null);
    }

    public static <T> ApiResponse<T> error(ApiErrorCode errorCode, T data) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), data);
    }

    public static ApiResponse<Void> error(ApiErrorCode errorCode, String message) {
        String resolvedMessage = Objects.requireNonNullElse(message, errorCode.message());
        return new ApiResponse<>(errorCode.code(), resolvedMessage, null);
    }

    private static <T> ApiResponse<T> success(ApiSuccessCode successCode, T data) {
        return new ApiResponse<>(successCode.code(), successCode.message(), data);
    }
}
