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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 提交冷却：验证"检查 + 记录"合并为一次 SET NX EX 后的行为。
 *
 * <p>改造前这两步是分开的，同一个学生的并发请求可能都通过检查；且状态存在单个实例的
 * 静态 Map 里，换一个实例即可绕过冷却。
 */
@Testcontainers
class SubmitCooldownStoreTest {

    private static final Duration COOLDOWN = Duration.ofSeconds(30);

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

    private SubmitCooldownStore store() {
        return new SubmitCooldownStore(new RedisStateStore(redis));
    }

    @Test
    void firstSubmitIsAllowedAndImmediateRepeatIsBlocked() {
        SubmitCooldownStore store = store();

        assertEquals(Duration.ZERO, store.acquire(1L, 100L, COOLDOWN));

        Duration remaining = store.acquire(1L, 100L, COOLDOWN);
        assertTrue(remaining.getSeconds() > 0, "冷却期内的再次提交必须被拒绝并给出剩余时间");
        assertTrue(remaining.getSeconds() <= COOLDOWN.getSeconds());
    }

    @Test
    void cooldownIsScopedPerStudentAndPerProblem() {
        SubmitCooldownStore store = store();
        assertEquals(Duration.ZERO, store.acquire(1L, 100L, COOLDOWN));

        assertEquals(Duration.ZERO, store.acquire(1L, 200L, COOLDOWN), "换一道题不应受同一学生的影响");
        assertEquals(Duration.ZERO, store.acquire(2L, 100L, COOLDOWN), "换一个学生不应受影响");
        assertTrue(store.acquire(1L, 100L, COOLDOWN).getSeconds() > 0);
    }

    @Test
    void cooldownIsSharedAcrossInstancesThroughRedis() {
        assertEquals(Duration.ZERO, store().acquire(3L, 100L, COOLDOWN));

        // 另一个实例：独立对象，只共享 Redis
        assertTrue(store().acquire(3L, 100L, COOLDOWN).getSeconds() > 0,
                "冷却必须在实例间共用，否则换实例即可绕过");
    }

    @Test
    void cooldownKeyCarriesTheCooldownAsTtl() {
        store().acquire(4L, 100L, COOLDOWN);

        Long ttlSeconds = redis.getExpire(RedisKeys.submitCooldown(4L, 100L), TimeUnit.SECONDS);
        assertTrue(ttlSeconds != null && ttlSeconds > 0 && ttlSeconds <= COOLDOWN.getSeconds(),
                "冷却标记必须带 TTL，否则条目只增不减，实际=" + ttlSeconds);
    }

    @Test
    void expiredCooldownAllowsTheNextSubmit() throws InterruptedException {
        SubmitCooldownStore store = store();
        assertEquals(Duration.ZERO, store.acquire(5L, 100L, Duration.ofMillis(200)));

        Thread.sleep(400);

        assertEquals(Duration.ZERO, store.acquire(5L, 100L, Duration.ofMillis(200)),
                "冷却到期后必须重新放行");
    }
}
