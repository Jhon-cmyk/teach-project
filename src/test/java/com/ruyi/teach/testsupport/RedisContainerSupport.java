package com.ruyi.teach.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试共用的 Redis 容器。
 *
 * <p>为什么不能让集成测试连本地 Redis：很多断言直接依赖 Redis 的原子语义与 TTL
 * （验证码一次性消费、登出后 Token 立即失效、滑动窗口计数、提交冷却）。
 * 若沿用内存实现或连开发机上的实例，测试之间会互相污染，也验证不到真实行为。
 *
 * <p>与 {@code application-test.yml} 中的 fail-closed 占位配置配套：若容器未启动，
 * 测试会连 127.0.0.1:1 并快速失败，而不是悄悄打到开发者的 Redis 上。
 *
 * <p>容器在此类中显式启动而非用 {@code @Container} 注解：Testcontainers 的 JUnit
 * 扩展只扫描测试类自身声明的字段，放在支持类里的注解不会生效，会静默不启动容器。
 */
public final class RedisContainerSupport {

    public static final String REDIS_IMAGE = "redis:7.4-alpine";

    private static GenericContainer<?> redis;

    private RedisContainerSupport() {
    }

    /** 启动容器（同一 JVM 内只启动一次），必须在读取映射端口之前调用。 */
    public static synchronized GenericContainer<?> container() {
        if (redis == null) {
            redis = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
                    .withExposedPorts(6379)
                    .withReuse(false);
            redis.start();
        }
        return redis;
    }

    /** 把容器地址注册进 Spring 配置，必须在 {@code @DynamicPropertySource} 中调用。 */
    public static void register(DynamicPropertyRegistry registry) {
        GenericContainer<?> container = container();
        registry.add("spring.data.redis.host", container::getHost);
        registry.add("spring.data.redis.port", () -> container.getMappedPort(6379));
    }
}
