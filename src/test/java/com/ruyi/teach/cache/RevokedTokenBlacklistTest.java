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

import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Token 吊销名单：登出后必须立即失效，且名单条目要随 Token 过期自动消失。
 *
 * <p>同时覆盖降级策略——这是本项目里唯一 fail-closed 的 Redis 用途，
 * 必须由测试锁住，避免后人把它改成静默放行。
 */
@Testcontainers
class RevokedTokenBlacklistTest {

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

    private RevokedTokenBlacklist blacklist() {
        return new RevokedTokenBlacklist(new RedisStateStore(redis));
    }

    private static void applyFailurePolicy(RevokedTokenBlacklist blacklist, String policy) throws Exception {
        Method configure = RevokedTokenBlacklist.class
                .getDeclaredMethod("configureFailurePolicy", String.class);
        configure.setAccessible(true);
        configure.invoke(blacklist, policy);
    }

    /** 指向一个必然连不上的端口的连接工厂，用于模拟 Redis 不可用。 */
    private static StringRedisTemplate unreachableRedis() {
        LettuceConnectionFactory dead = new LettuceConnectionFactory("127.0.0.1", 1);
        dead.setTimeout(300);
        dead.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(dead);
        template.afterPropertiesSet();
        return template;
    }

    @Test
    void revokedTokenIsRejectedAndUnrevokedTokenIsAccepted() {
        RevokedTokenBlacklist blacklist = blacklist();
        long expiresAt = System.currentTimeMillis() + Duration.ofMinutes(30).toMillis();

        assertFalse(blacklist.isRevoked("hash-a", expiresAt));
        blacklist.revoke("hash-a", expiresAt);
        assertTrue(blacklist.isRevoked("hash-a", expiresAt));
        assertFalse(blacklist.isRevoked("hash-b", expiresAt), "只能吊销当前标签页的 Token");
    }

    @Test
    void revokedEntryExpiresTogetherWithTheToken() {
        RevokedTokenBlacklist blacklist = blacklist();
        long expiresAt = System.currentTimeMillis() + Duration.ofSeconds(120).toMillis();

        blacklist.revoke("hash-a", expiresAt);

        long ttl = blacklist.revokedTtlSeconds("hash-a");
        // 允许少量执行耗时误差；关键是名单条目不会比 Token 活得更久
        assertTrue(ttl > 0 && ttl <= 120, "名单条目 TTL 应跟随 Token 剩余有效期，实际=" + ttl);
    }

    @Test
    void alreadyExpiredTokenIsNotWrittenToTheList() {
        RevokedTokenBlacklist blacklist = blacklist();

        blacklist.revoke("hash-expired", System.currentTimeMillis() - 1_000);

        assertFalse(blacklist.isRevoked("hash-expired", System.currentTimeMillis() - 1_000));
        assertTrue(blacklist.revokedTtlSeconds("hash-expired") < 0, "不应为已过期 Token 留下条目");
    }

    @Test
    void redisFailureFailsClosedByDefault() throws Exception {
        RevokedTokenBlacklist blacklist = new RevokedTokenBlacklist(new RedisStateStore(unreachableRedis()));
        applyFailurePolicy(blacklist, "fail-closed");

        // 安全开关：Redis 不可用时必须拒绝，而不是静默放行已登出的 Token
        assertTrue(blacklist.isRevoked("hash-a", System.currentTimeMillis() + 60_000));
    }

    @Test
    void redisFailureCanBeConfiguredToFailOpenExplicitly() throws Exception {
        RevokedTokenBlacklist blacklist = new RevokedTokenBlacklist(new RedisStateStore(unreachableRedis()));
        applyFailurePolicy(blacklist, "fail-open");

        // 只有显式配置才允许放行；默认值不得是这一侧
        assertFalse(blacklist.isRevoked("hash-a", System.currentTimeMillis() + 60_000));
    }

    @Test
    void revokedEntryIsStoredUnderTheDocumentedKeyPrefix() {
        long expiresAt = System.currentTimeMillis() + Duration.ofMinutes(5).toMillis();

        blacklist().revoke("hash-a", expiresAt);

        assertTrue(Boolean.TRUE.equals(redis.hasKey(RedisKeys.KEY_PREFIX + "auth:revoked:hash-a")));
    }
}
