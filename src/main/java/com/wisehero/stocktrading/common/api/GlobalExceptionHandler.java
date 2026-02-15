package com.wisehero.stocktrading.common.api;

import com.wisehero.stocktrading.common.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ApiResponse<Void> handleApiException(ApiException exception, HttpServletResponse response) {
        response.setStatus(exception.getErrorCode().status().value());
        return ApiResponse.error(exception.getErrorCode().code(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<ValidationErrorData> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletResponse response
    ) {
        response.setStatus(ApiErrorCode.BAD_REQUEST.status().value());

        List<ValidationErrorDetail> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ValidationErrorDetail(
                        fieldError.getField(),
                        Objects.requireNonNullElse(fieldError.getDefaultMessage(), "유효하지 않은 값입니다.")
                ))
                .toList();

        return ApiResponse.error(ApiErrorCode.BAD_REQUEST, new ValidationErrorData(errors));
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<ValidationErrorData> handleBindException(BindException exception, HttpServletResponse response) {
        response.setStatus(ApiErrorCode.BAD_REQUEST.status().value());

        List<ValidationErrorDetail> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ValidationErrorDetail(
                        fieldError.getField(),
                        Objects.requireNonNullElse(fieldError.getDefaultMessage(), "유효하지 않은 값입니다.")
                ))
                .toList();

        return ApiResponse.error(ApiErrorCode.BAD_REQUEST, new ValidationErrorData(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<ValidationErrorData> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletResponse response
    ) {
        response.setStatus(ApiErrorCode.BAD_REQUEST.status().value());

        List<ValidationErrorDetail> errors = exception.getConstraintViolations()
                .stream()
                .map(this::toValidationError)
                .toList();

        return ApiResponse.error(ApiErrorCode.BAD_REQUEST, new ValidationErrorData(errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<ValidationErrorData> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletResponse response
    ) {
        response.setStatus(ApiErrorCode.BAD_REQUEST.status().value());

        ValidationErrorDetail error = new ValidationErrorDetail(
                exception.getName(),
                "요청 값의 타입이 올바르지 않습니다."
        );

        return ApiResponse.error(ApiErrorCode.BAD_REQUEST, new ValidationErrorData(List.of(error)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<ValidationErrorData> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletResponse response
    ) {
        response.setStatus(ApiErrorCode.BAD_REQUEST.status().value());

        ValidationErrorDetail error = new ValidationErrorDetail(
                exception.getParameterName(),
                "필수 요청 파라미터가 누락되었습니다."
        );

        return ApiResponse.error(ApiErrorCode.BAD_REQUEST, new ValidationErrorData(List.of(error)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Void> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception,
            HttpServletResponse response
    ) {
        response.setStatus(ApiErrorCode.METHOD_NOT_ALLOWED.status().value());
        return ApiResponse.error(ApiErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletResponse response
    ) {
        response.setStatus(ApiErrorCode.NOT_FOUND.status().value());
        return ApiResponse.error(ApiErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception, HttpServletResponse response) {
        response.setStatus(ApiErrorCode.INTERNAL_SERVER_ERROR.status().value());
        return ApiResponse.error(ApiErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ValidationErrorDetail toValidationError(ConstraintViolation<?> violation) {
        return new ValidationErrorDetail(
                violation.getPropertyPath().toString(),
                violation.getMessage()
        );
    }
}
