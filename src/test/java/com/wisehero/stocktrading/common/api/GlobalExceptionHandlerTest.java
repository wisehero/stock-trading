package com.wisehero.stocktrading.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestApiController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successResponseShouldFollowStandardFormat() throws Exception {
        mockMvc.perform(get("/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
                .andExpect(jsonPath("$.data.value").value("pong"));
    }

    @Test
    void validationFailureShouldReturnBadRequestWithErrorData() throws Exception {
        TestApiController.CreateRequest request = new TestApiController.CreateRequest("");

        mockMvc.perform(post("/test/validate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.data.errors[0].field").value("name"));
    }

    @Test
    void apiExceptionShouldKeepMappedHttpStatus() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON-404"))
                .andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."));
    }

    @Test
    void typeMismatchShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/test/path/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    void unexpectedExceptionShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON-500"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    void unsupportedMethodShouldReturnMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/test/method-only"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("COMMON-405"))
                .andExpect(jsonPath("$.message").value("허용되지 않은 HTTP 메서드입니다."));
    }

    @Test
    void unknownPathShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/test/unknown-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON-404"))
                .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다."));
    }

}
