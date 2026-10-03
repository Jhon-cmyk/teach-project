package com.ruyi.teach.config;

import com.ruyi.teach.controller.SessionUserContext;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /ai/** 接口的按用户滑动窗口限流。
 *
 * <p>这些接口每次调用都会真实请求付费大模型，仅校验登录不足以防止单个账号
 * 循环调用造成费用放大，因此在此限制单位时间内的请求次数。
 *
 * <p>限流键为登录用户 ID，因此必须在认证拦截器之后执行（见 WebMvcConfig 注册顺序）。
 */
@Component
public class AiRateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_MS = 60_000L;

    /** 极端情况下的兜底，避免长期运行后 map 无界增长。 */
    private static final int MAX_TRACKED_USERS = 10_000;

    private final Map<Long, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    /** 每分钟允许的请求数；配置为 0 或负数表示关闭限流。 */
    @Value("${ai.rate-limit.requests-per-minute:40}")
    private int requestsPerMinute;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || requestsPerMinute <= 0) {
            return true;
        }

        User user = SessionUserContext.getOptional(request);
        if (user == null || user.getId() == null) {
            // 未登录请求由认证拦截器负责拒绝，这里不重复处理
            return true;
        }

        if (!allow(user.getId(), System.currentTimeMillis())) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR,
                    "AI 请求过于频繁，请稍后再试（每分钟最多 " + requestsPerMinute + " 次）"
            );
        }
        return true;
    }

    private boolean allow(Long userId, long now) {
        if (requestLog.size() > MAX_TRACKED_USERS) {
            requestLog.clear();
        }

        Deque<Long> timestamps = requestLog.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (timestamps) {
            long cutoff = now - WINDOW_MS;
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= requestsPerMinute) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
