package com.ruyi.teach.config;

import com.ruyi.teach.cache.AiRateLimitGuard;
import com.ruyi.teach.controller.SessionUserContext;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拦截器职责：决定哪些请求进入限流、额度用尽时报什么错。
 * 滑动窗口本身的原子性由 Redis 侧实现保证，属于 {@code AiRateLimitGuard} 的测试范围。
 */
class AiRateLimitInterceptorTest {

    private AiRateLimitGuard guard;
    private long lastUserId;
    private int lastRequestsPerMinute;

    @BeforeEach
    void setUp() {
        guard = mock(AiRateLimitGuard.class);
        lastUserId = -1L;
        lastRequestsPerMinute = -1;
        when(guard.allow(anyLong(), anyInt(), anyLong())).thenAnswer(invocation -> {
            lastUserId = invocation.getArgument(0);
            lastRequestsPerMinute = invocation.getArgument(1);
            return true;
        });
    }

    private AiRateLimitInterceptor interceptorWithLimit(int requestsPerMinute) {
        AiRateLimitInterceptor interceptor = new AiRateLimitInterceptor(guard);
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
        when(guard.allow(anyLong(), anyInt(), anyLong())).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> call(interceptor, 1L)
        );
        assertEquals(50001, exception.getCode());
    }

    @Test
    void passesTheConfiguredLimitAndLoginUserIdToTheLimiter() {
        AiRateLimitInterceptor interceptor = interceptorWithLimit(7);

        assertDoesNotThrow(() -> call(interceptor, 42L));

        assertEquals(42L, lastUserId);
        assertEquals(7, lastRequestsPerMinute);
        verify(guard).allow(eq(42L), eq(7), anyLong());
    }

    @Test
    void nonPositiveLimitDisablesRateLimiting() {
        AiRateLimitInterceptor interceptor = interceptorWithLimit(0);

        for (int i = 0; i < 50; i++) {
            assertDoesNotThrow(() -> call(interceptor, 1L));
        }

        verify(guard, never()).allow(anyLong(), anyInt(), anyLong());
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

        verify(guard, never()).allow(anyLong(), anyInt(), anyLong());
    }
}
