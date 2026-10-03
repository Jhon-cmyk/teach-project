package com.ruyi.teach.cache;

import com.ruyi.teach.service.RevokedTokenStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 Token 吊销名单。
 *
 * <p>改造前该名单是进程内的 {@code ConcurrentHashMap}，导致在 A 实例退出登录后，
 * 旧 Token 在 B 实例仍然有效——这正是引入 Redis 要解决的一致性问题。
 *
 * <p>条目 TTL 直接取 Token 的剩余有效期：Token 一旦自然过期，名单条目也随之消失，
 * 既不需要手动清理，也不会让名单无限增长。
 *
 * <h2>降级策略</h2>
 * <p>这是一个<b>安全开关</b>：它的作用是让已登出/疑似泄露的 Token 立刻失效。
 * 因此 Redis 不可用时默认 {@code fail-closed}（拒绝该 Token），而不是悄悄放行——
 * 静默放行等于把安全开关关掉，而调用方往往意识不到。
 *
 * <p>若部署方明确认为可用性优先，可把 {@code auth.token.blacklist-failure-policy}
 * 显式设为 {@code fail-open}（放行并告警）。这个取舍必须是显式配置，不能是默认行为。
 */
@Component
public class RevokedTokenBlacklist implements RevokedTokenStore {

    private static final String FAIL_OPEN = "fail-open";

    private final RedisStateStore store;

    /** Redis 不可用时是否放行；默认 fail-closed。 */
    private boolean failOpen;

    public RevokedTokenBlacklist(RedisStateStore store) {
        this.store = store;
    }

    @Value("${auth.token.blacklist-failure-policy:fail-closed}")
    void configureFailurePolicy(String policy) {
        this.failOpen = FAIL_OPEN.equalsIgnoreCase(policy == null ? "" : policy.trim());
    }

    @Override
    public boolean isRevoked(String tokenHash, long tokenExpiresAt) {
        StringRedisTemplate redis = store.template();
        // 降级值即"是否拒绝该 Token"：fail-closed 时 Redis 不可用一律视为已吊销
        return store.withRedis(
                "Token 吊销校验",
                () -> Boolean.TRUE.equals(redis.hasKey(RedisKeys.revokedToken(tokenHash))),
                !failOpen
        );
    }

    @Override
    public void revoke(String tokenHash, long tokenExpiresAt) {
        long remainingMillis = tokenExpiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            // Token 本身已过期，它已经无法通过 resolve 中的有效期校验，无需写入名单
            return;
        }

        StringRedisTemplate redis = store.template();
        store.withRedis(
                "Token 吊销写入",
                () -> {
                    redis.opsForValue().setIfAbsent(
                            RedisKeys.revokedToken(tokenHash),
                            "1",
                            Duration.ofMillis(remainingMillis)
                    );
                    return Boolean.TRUE;
                },
                Boolean.FALSE
        );
    }

    /** 便于测试与排查：当前条目的剩余存活时间，不存在或 Redis 不可用时返回负值。 */
    public long revokedTtlSeconds(String tokenHash) {
        Long ttl = store.withRedis(
                "Token 吊销 TTL 查询",
                () -> store.template().getExpire(RedisKeys.revokedToken(tokenHash), TimeUnit.SECONDS),
                -2L
        );
        return ttl == null ? -2L : ttl;
    }
}
