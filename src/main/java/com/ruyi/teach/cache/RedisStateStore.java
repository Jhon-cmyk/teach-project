package com.ruyi.teach.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Redis 访问的唯一入口，负责把"Redis 不可用"这件事显式化。
 *
 * <p>为什么要包一层：四个改造点的降级要求并不相同——Token 黑名单是安全开关（不可用时应
 * 拒绝），限流与提交冷却是可用性优先（不可用时应放行）。如果每个调用点各自 try/catch，
 * 迟早会有人把安全开关随手 catch 掉。因此这里统一捕获异常并记日志，由调用方通过
 * {@code fallback} 显式声明"Redis 挂了算哪一边"，每个调用点的策略都写在代码里可被审查。
 */
@Slf4j
@Component
public class RedisStateStore {

    private final StringRedisTemplate redis;

    public RedisStateStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public StringRedisTemplate template() {
        return redis;
    }

    /**
     * 执行一次 Redis 操作，失败时返回调用方指定的降级值。
     *
     * @param operation 描述性名称，仅用于日志定位是哪个用途在降级
     * @param action    Redis 操作
     * @param fallback  Redis 不可用时的返回值，即该用途的降级策略
     */
    public <T> T withRedis(String operation, Supplier<T> action, T fallback) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            log.warn("Redis 不可用，按降级策略继续（用途={}，降级值={}）: {}",
                    operation, fallback, e.getMessage());
            return fallback;
        }
    }
}
