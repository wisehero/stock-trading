package com.wisehero.stocktrading.common.api;

import com.wisehero.stocktrading.common.exception.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/test")
public class TestApiController {

    @GetMapping("/success")
    public ApiResponse<TestData> success() {
        return ApiResponse.ok(new TestData("pong"));
    }

    @PostMapping("/validate")
    public ApiResponse<TestData> validate(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.created(new TestData(request.name()));
    }

    @GetMapping("/not-found")
    public ApiResponse<Void> notFound() {
        throw new ApiException(ApiErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다.");
    }

    @GetMapping("/path/{id}")
    public ApiResponse<Void> path(@PathVariable long id) {
        return ApiResponse.ok();
    }

    @GetMapping("/error")
    public ApiResponse<Void> error() {
        throw new IllegalStateException("boom");
    }

    @GetMapping("/method-only")
    public ApiResponse<Void> methodOnly() {
        return ApiResponse.ok();
    }

    @GetMapping("/required-param")
    public ApiResponse<Void> requiredParam(@RequestParam String symbol) {
        return ApiResponse.ok();
    }

    public record CreateRequest(@NotBlank(message = "name은 필수입니다.") String name) {
    }

    public record TestData(String value) {
    }
}
