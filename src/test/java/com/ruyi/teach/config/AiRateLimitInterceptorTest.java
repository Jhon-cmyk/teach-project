package com.ruyi.teach.config;

import com.ruyi.teach.controller.SessionUserContext;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiRateLimitInterceptorTest {

    private AiRateLimitInterceptor interceptorWithLimit(int requestsPerMinute) {
        AiRateLimitInterceptor interceptor = new AiRateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "requestsPerMinute", requestsPerMinute);
        return interceptor;
    }

    private MockHttpServletRequest requestFor(long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/stream");
        request.setContextPath("/api");
        User user = new User();
        user.setId(userId);
        user.setUserRole("student");
        SessionUserContext.login(request, user);
        return request;
    }

    private boolean call(AiRateLimitInterceptor interceptor, long userId) {
        return interceptor.preHandle(requestFor(userId), new MockHttpServletResponse(), new Object());
    }

    @Test
    void rejectsRequestsBeyondThePerMinuteLimit() {
        AiRateLimitInterceptor interceptor = interceptorWithLimit(3);

        assertDoesNotThrow(() -> call(interceptor, 1L));
        assertDoesNotThrow(() -> call(interceptor, 1L));
        assertDoesNotThrow(() -> call(interceptor, 1L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> call(interceptor, 1L)
        );
        assertEquals(50001, exception.getCode());
    }

    @Test
    void countsEachUserIndependently() {
        AiRateLimitInterceptor interceptor = interceptorWithLimit(1);

        assertDoesNotThrow(() -> call(interceptor, 1L));
        assertThrows(BusinessException.class, () -> call(interceptor, 1L));
        // 另一个用户不应被前一个用户的用量影响
        assertDoesNotThrow(() -> call(interceptor, 2L));
    }

    @Test
    void nonPositiveLimitDisablesRateLimiting() {
        AiRateLimitInterceptor interceptor = interceptorWithLimit(0);

        for (int i = 0; i < 50; i++) {
            assertDoesNotThrow(() -> call(interceptor, 1L));
        }
    }

    @Test
    void anonymousRequestsAreLeftToTheAuthenticationInterceptor() {
        AiRateLimitInterceptor interceptor = interceptorWithLimit(1);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/stream");
        request.setContextPath("/api");

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(
                    () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
            );
        }
    }
}
