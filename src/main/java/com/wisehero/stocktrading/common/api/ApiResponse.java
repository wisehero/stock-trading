package com.wisehero.stocktrading.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/**
 * Standard API response envelope.
 * <p>
 * Response shape: {@code {code, message, data}}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String code, String message, T data) {

    /**
     * Creates a success response with HTTP-200 semantic code.
     */
    public static <T> ApiResponse<T> ok(T data) {
        return success(ApiSuccessCode.OK, data);
    }

    /**
     * Creates a success response without payload.
     */
    public static ApiResponse<Void> ok() {
        return success(ApiSuccessCode.OK, null);
    }

    /**
     * Creates a success response with HTTP-201 semantic code.
     */
    public static <T> ApiResponse<T> created(T data) {
        return success(ApiSuccessCode.CREATED, data);
    }

    /**
     * Creates a success response with HTTP-204 semantic code.
     */
    public static ApiResponse<Void> noContent() {
        return success(ApiSuccessCode.NO_CONTENT, null);
    }

    /**
     * Creates an error response with default message from {@link ApiErrorCode}.
     */
    public static ApiResponse<Void> error(ApiErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null);
    }

    /**
     * Creates an error response that includes structured error payload.
     */
    public static <T> ApiResponse<T> error(ApiErrorCode errorCode, T data) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), data);
    }

    /**
     * Creates an error response with an overridden message.
     */
    public static ApiResponse<Void> error(ApiErrorCode errorCode, String message) {
        String resolvedMessage = Objects.requireNonNullElse(message, errorCode.message());
        return new ApiResponse<>(errorCode.code(), resolvedMessage, null);
    }

    private static <T> ApiResponse<T> success(ApiSuccessCode successCode, T data) {
        return new ApiResponse<>(successCode.code(), successCode.message(), data);
    }
}
