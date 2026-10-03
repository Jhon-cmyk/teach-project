package com.ruyi.teach.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * /ai/** 接口的按用户滑动窗口限流（Redis 实现）。
 *
 * <p>这些接口每次调用都会真实请求付费大模型，仅校验登录不足以防止单个账号循环调用
 * 造成费用放大，因此必须限流。改造前窗口存在各实例的 JVM 内存里，多实例下实际上限会
 * 放大为"实例数 × 每分钟额度"，而且超过 1 万个用户时会把所有人的计数一起清空。
 *
 * <h2>为什么必须用 Lua</h2>
 * <p>滑动窗口需要"清理过期记录 → 计数 → 记录本次请求"三步。若用三条独立命令实现，
 * 两步之间就存在竞态：多实例并发时恰好会突破上限，而这正是本次改造要解决的问题。
 * 脚本在 Redis 中单线程原子执行，才能保证窗口计数准确。
 *
 * <h2>降级策略</h2>
 * <p>限流是<b>成本控制手段，不是安全边界</b>。Redis 不可用时放行（fail-open）并告警，
 * 避免一次 Redis 抖动导致全部 AI 接口不可用；这与 Token 黑名单的 fail-closed 有意不同，
 * 两者策略差异是明确设计而非疏漏。
 */
@Slf4j
@Component
public class AiRateLimitGuard {

    /**
     * KEYS[1] 限流键；ARGV[1] 当前时间戳（毫秒）；ARGV[2] 窗口长度（毫秒）；
     * ARGV[3] 窗口内允许的最大请求数；ARGV[4] 请求标识（毫秒时间戳 + 随机后缀）。
     *
     * <p>member 不使用纯时间戳：同一毫秒内的并发请求会因 member 相同而互相覆盖，
     * 导致计数偏少、限流被突破。
     */
    private static final String SCRIPT = """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1] - ARGV[2])
            local used = redis.call('ZCARD', KEYS[1])
            if used >= tonumber(ARGV[3]) then
                return 0
            end
            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
            -- 略长于窗口，避免键在窗口边界被过早回收
            redis.call('PEXPIRE', KEYS[1], ARGV[2] + 10000)
            return 1
            """;

    private static final RedisScript<Long> SLIDING_WINDOW_SCRIPT =
            new DefaultRedisScript<>(SCRIPT, Long.class);

    private final RedisStateStore store;

    /** 滑动窗口长度。 */
    private Duration window = Duration.ofMinutes(1);

    public AiRateLimitGuard(RedisStateStore store) {
        this.store = store;
    }

    @Value("${ai.rate-limit.window:60s}")
    void configureWindow(Duration configuredWindow) {
        if (configuredWindow == null || configuredWindow.isZero() || configuredWindow.isNegative()) {
            throw new IllegalArgumentException("AI rate limit window must be positive");
        }
        this.window = configuredWindow;
    }

    /**
     * 判断本次请求是否放行。
     *
     * @param userId            登录用户 ID
     * @param requestsPerMinute 窗口内允许的请求数
     * @param now               当前时间戳（毫秒），由调用方传入以便测试
     * @return true 放行；false 表示已超出窗口额度
     */
    public boolean allow(Long userId, int requestsPerMinute, long now) {
        if (userId == null || requestsPerMinute <= 0) {
            return true;
        }

        StringRedisTemplate redis = store.template();
        List<String> keys = Collections.singletonList(RedisKeys.aiRateLimit(userId));
        String member = now + "-" + Long.toHexString(System.nanoTime());

        Long allowed = store.withRedis(
                "AI 限流计数",
                () -> redis.execute(
                        SLIDING_WINDOW_SCRIPT,
                        keys,
                        String.valueOf(now),
                        String.valueOf(window.toMillis()),
                        String.valueOf(requestsPerMinute),
                        member
                ),
                null
        );

        if (allowed == null) {
            // Redis 不可用：放行，避免全站 AI 接口不可用。日志已由 RedisStateStore 记录。
            return true;
        }
        return allowed > 0;
    }
}
