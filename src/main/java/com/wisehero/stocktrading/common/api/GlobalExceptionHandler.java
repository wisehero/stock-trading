package com.wisehero.stocktrading.common.api;

import com.wisehero.stocktrading.common.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DEFAULT_VALIDATION_MESSAGE = "유효하지 않은 값입니다.";
    private static final String INVALID_TYPE_MESSAGE = "요청 값의 타입이 올바르지 않습니다.";
    private static final String MISSING_PARAMETER_MESSAGE = "필수 요청 파라미터가 누락되었습니다.";

    @ExceptionHandler(ApiException.class)
    public ApiResponse<Void> handleApiException(ApiException exception, HttpServletResponse response) {
        ApiErrorCode errorCode = exception.getErrorCode();
        response.setStatus(errorCode.status().value());
        return ApiResponse.error(errorCode, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<ValidationErrorData> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletResponse response
    ) {
        return badRequest(response, toFieldErrors(exception.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<ValidationErrorData> handleBindException(BindException exception, HttpServletResponse response) {
        return badRequest(response, toFieldErrors(exception.getBindingResult()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<ValidationErrorData> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletResponse response
    ) {
        List<ValidationErrorDetail> errors = exception.getConstraintViolations()
                .stream()
                .map(this::toValidationError)
                .toList();

        return badRequest(response, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<ValidationErrorData> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletResponse response
    ) {
        ValidationErrorDetail error = new ValidationErrorDetail(
                exception.getName(),
                INVALID_TYPE_MESSAGE
        );

        return badRequest(response, List.of(error));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<ValidationErrorData> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletResponse response
    ) {
        ValidationErrorDetail error = new ValidationErrorDetail(
                exception.getParameterName(),
                MISSING_PARAMETER_MESSAGE
        );

        return badRequest(response, List.of(error));
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
        log.error("Unhandled exception occurred", exception);
        response.setStatus(ApiErrorCode.INTERNAL_SERVER_ERROR.status().value());
        return ApiResponse.error(ApiErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ApiResponse<ValidationErrorData> badRequest(
            HttpServletResponse response,
            List<ValidationErrorDetail> errors
    ) {
        response.setStatus(ApiErrorCode.BAD_REQUEST.status().value());
        return ApiResponse.error(ApiErrorCode.BAD_REQUEST, new ValidationErrorData(errors));
    }

    private List<ValidationErrorDetail> toFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors()
                .stream()
                .map(fieldError -> new ValidationErrorDetail(
                        fieldError.getField(),
                        Objects.requireNonNullElse(fieldError.getDefaultMessage(), DEFAULT_VALIDATION_MESSAGE)
                ))
                .toList();
    }

    private ValidationErrorDetail toValidationError(ConstraintViolation<?> violation) {
        return new ValidationErrorDetail(
                extractFieldPath(violation.getPropertyPath().toString()),
                violation.getMessage()
        );
    }

    private String extractFieldPath(String rawPath) {
        int lastDot = rawPath.lastIndexOf('.');
        return lastDot < 0 ? rawPath : rawPath.substring(lastDot + 1);
    }
}
