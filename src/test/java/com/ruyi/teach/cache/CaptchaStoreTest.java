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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证码的 Redis 存储：跨实例可见 + 一次性消费 + 浏览器绑定跨实例可验证。
 *
 * <p>这几点正是引入 Redis 的原因：改造前验证码存在单个实例的 Session 里，
 * 取码与校验落到不同实例时会永远校验失败，且没有原子的一次性消费语义。
 */
@Testcontainers
class CaptchaStoreTest {

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

    private CaptchaStore store() {
        return new CaptchaStore(new RedisStateStore(redis));
    }

    @Test
    void captchaIssuedOnOneInstanceIsVerifiableOnAnother() {
        String captchaId = store().create("a1b2");
        assertNotNull(captchaId);

        // 另一个实例：独立对象，只共享 Redis
        assertEquals("a1b2", store().consume(captchaId));
    }

    @Test
    void captchaCanOnlyBeConsumedOnce() {
        String captchaId = store().create("a1b2");

        assertEquals("a1b2", store().consume(captchaId));
        assertNull(store().consume(captchaId), "GETDEL 必须保证验证码不可重放");
    }

    @Test
    void unknownCaptchaReturnsNull() {
        assertNull(store().consume("no-such-captcha-id"));
    }

    /**
     * 回归测试：绑定最初写在 HttpSession 里，导致"A 取码、B 校验"必然失败
     * （B 读不到 A 的 Session）。现在绑定是签名令牌，必须能被任意实例独立验证。
     */
    @Test
    void bindingIssuedOnOneInstanceIsVerifiableOnAnotherWithoutSharedSession() {
        CaptchaStore instanceA = store();
        CaptchaStore instanceB = store();

        String captchaId = instanceA.create("a1b2");
        String binding = instanceA.issueBinding(captchaId);

        assertTrue(instanceB.verifyBinding(captchaId, binding),
                "绑定必须跨实例可验证，否则验证码在负载均衡后会失效");
    }

    @Test
    void bindingIsRejectedWhenCaptchaIdOrTokenDoesNotMatch() {
        CaptchaStore captchaStore = store();
        String captchaId = captchaStore.create("a1b2");
        String otherCaptchaId = captchaStore.create("c3d4");

        String binding = captchaStore.issueBinding(captchaId);

        assertFalse(captchaStore.verifyBinding(otherCaptchaId, binding), "绑定不能挪用到别的 captchaId");
        assertFalse(captchaStore.verifyBinding(captchaId, null));
        assertFalse(captchaStore.verifyBinding(captchaId, ""));
        assertFalse(captchaStore.verifyBinding(captchaId, "not-a-token"));
        assertFalse(captchaStore.verifyBinding(captchaId, binding + "tampered"), "签名被篡改必须拒绝");
    }

    @Test
    void bindingExpiresWithTheCaptcha() throws Exception {
        CaptchaStore captchaStore = store();
        java.lang.reflect.Method configureTtl =
                CaptchaStore.class.getDeclaredMethod("configureTtl", Duration.class);
        configureTtl.setAccessible(true);
        configureTtl.invoke(captchaStore, Duration.ofMillis(150));

        String captchaId = captchaStore.create("a1b2");
        String binding = captchaStore.issueBinding(captchaId);
        assertTrue(captchaStore.verifyBinding(captchaId, binding));

        Thread.sleep(300);

        assertFalse(captchaStore.verifyBinding(captchaId, binding), "过期绑定必须失效");
    }

    @Test
    void captchaCarriesATtl() {
        String captchaId = store().create("a1b2");

        Long ttlSeconds = redis.getExpire(RedisKeys.captcha(captchaId), TimeUnit.SECONDS);
        assertNotNull(ttlSeconds);
        // 配置为 5 分钟；这里只校验"确实设置了正数 TTL"，避免与具体配置耦合
        assertTrue(
                ttlSeconds > 0 && ttlSeconds <= 300,
                "验证码必须带 TTL，实际=" + ttlSeconds
        );
    }
}
