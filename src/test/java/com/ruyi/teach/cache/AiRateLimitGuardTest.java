package com.ruyi.teach.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 限流的 Redis 滑动窗口：验证多实例语义（计数共用）与 Lua 的原子性。
 */
@Testcontainers
class AiRateLimitGuardTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(false);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    private AiRateLimitGuard guard() {
        return new AiRateLimitGuard(new RedisStateStore(redis));
    }

    @Test
    void blocksRequestsBeyondTheLimitWithinTheWindow() {
        AiRateLimitGuard guard = guard();
        long now = System.currentTimeMillis();

        assertTrue(guard.allow(1L, 3, now));
        assertTrue(guard.allow(1L, 3, now));
        assertTrue(guard.allow(1L, 3, now));
        assertFalse(guard.allow(1L, 3, now), "第 4 次请求应被拒绝");
    }

    @Test
    void sameMillisecondRequestsAreCountedSeparately() {
        AiRateLimitGuard guard = guard();
        long now = System.currentTimeMillis();

        // member 若只用时间戳，同一毫秒的并发请求会互相覆盖而少计，导致限流被突破
        assertTrue(guard.allow(1L, 2, now));
        assertTrue(guard.allow(1L, 2, now));
        assertFalse(guard.allow(1L, 2, now), "同一毫秒内的第 3 次请求必须被拒绝");
    }

    @Test
    void requestCountIsSharedAcrossInstancesThroughRedis() {
        long now = System.currentTimeMillis();
        AiRateLimitGuard instanceA = guard();
        // 模拟另一个实例：不同对象、没有共享内存，只共享 Redis
        AiRateLimitGuard instanceB = guard();

        assertTrue(instanceA.allow(9L, 2, now));
        assertTrue(instanceB.allow(9L, 2, now));
        assertFalse(instanceA.allow(9L, 2, now),
                "额度必须在实例间共用，否则多实例下实际额度会被放大");
    }

    @Test
    void countsEachUserIndependently() {
        AiRateLimitGuard guard = guard();
        long now = System.currentTimeMillis();

        assertTrue(guard.allow(1L, 1, now));
        assertFalse(guard.allow(1L, 1, now));
        assertTrue(guard.allow(2L, 1, now), "另一个用户不应受前一个用户用量影响");
    }

    @Test
    void windowIsScopedToTheConfiguredDuration() throws Exception {
        AiRateLimitGuard guard = guard();
        long now = System.currentTimeMillis();

        // 缩短窗口以便快速验证"窗口外的旧记录不再计数"，而不是等待 60 秒
        java.lang.reflect.Method configureWindow =
                AiRateLimitGuard.class.getDeclaredMethod("configureWindow", Duration.class);
        configureWindow.setAccessible(true);
        configureWindow.invoke(guard, Duration.ofMillis(200));

        assertTrue(guard.allow(5L, 1, now));
        assertFalse(guard.allow(5L, 1, now));
        assertTrue(guard.allow(5L, 1, now + 500), "窗口外的旧记录应被清理后重新放行");
    }

    @Test
    void keyCarriesATtlSoAbandonedWindowsCannotAccumulate() {
        AiRateLimitGuard guard = guard();

        assertTrue(guard.allow(7L, 5, System.currentTimeMillis()));

        Long ttlSeconds = redis.getExpire(RedisKeys.aiRateLimit(7L), java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(ttlSeconds);
        assertTrue(ttlSeconds > 0, "限流键必须带 TTL，否则长期运行会无限增长");
    }
}
