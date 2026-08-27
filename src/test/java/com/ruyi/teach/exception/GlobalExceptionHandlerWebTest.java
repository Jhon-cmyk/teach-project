package com.ruyi.teach.exception;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.common.TraceContext;
import com.ruyi.teach.config.TraceIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerWebTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void businessErrorKeepsStableCodeAndTraceId() throws Exception {
        MvcResult result = mockMvc.perform(get("/error-test/business")
                        .header(TraceContext.HEADER_NAME, "client-trace-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceContext.HEADER_NAME, "client-trace-001"))
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("业务参数不正确"))
                .andExpect(jsonPath("$.traceId").value("client-trace-001"))
                .andReturn();

        org.junit.jupiter.api.Assertions.assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void unknownErrorDoesNotExposeInternalDetails() throws Exception {
        mockMvc.perform(get("/error-test/unknown"))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceContext.HEADER_NAME))
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(content().string(not(containsString("jdbc:mysql://internal"))))
                .andExpect(content().string(not(containsString("SELECT private_column"))));
    }

    @Test
    void externalServiceErrorHasDedicatedSafeCode() throws Exception {
        mockMvc.perform(get("/error-test/external"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50200))
                .andExpect(jsonPath("$.message").value("外部服务暂时不可用"))
                .andExpect(content().string(not(containsString("provider-internal-detail"))));
    }

    @Test
    void invalidParameterTypeReturnsParamsError() throws Exception {
        mockMvc.perform(get("/error-test/typed").param("id", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.data.id").value("参数类型错误"));
    }

    @Test
    void unsupportedMethodHasDedicatedCode() throws Exception {
        mockMvc.perform(post("/error-test/business"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40500))
                .andExpect(jsonPath("$.message").value("请求方法不支持"));
    }

    @Test
    void unsupportedMediaTypeHasDedicatedCode() throws Exception {
        mockMvc.perform(post("/error-test/json")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("text"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(41500))
                .andExpect(jsonPath("$.message").value("请求媒体类型不支持"));
    }

    @RestController
    @RequestMapping("/error-test")
    static class ErrorTestController {

        @GetMapping("/business")
        BaseResponse<Boolean> business() {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "业务参数不正确");
        }

        @GetMapping("/unknown")
        BaseResponse<Boolean> unknown() {
            throw new IllegalStateException(
                    "jdbc:mysql://internal SELECT private_column FROM internal_table"
            );
        }

        @GetMapping("/external")
        BaseResponse<Boolean> external() {
            throw new ExternalServiceException(
                    "test-provider",
                    new IllegalStateException("provider-internal-detail")
            );
        }

        @GetMapping("/typed")
        BaseResponse<Long> typed(@RequestParam Long id) {
            return ResultUtils.success(id);
        }

        @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        BaseResponse<Boolean> json() {
            return ResultUtils.success(true);
        }
    }
}
