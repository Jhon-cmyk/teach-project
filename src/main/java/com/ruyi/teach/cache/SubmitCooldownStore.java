package com.ruyi.teach.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 编程题提交冷却（Redis 实现）。
 *
 * <p>改造前是控制器里的静态 {@code ConcurrentHashMap}，存在两个问题：多实例下冷却可被
 * 绕过（换一个实例即可再提交一次），以及条目只增不减导致长期运行内存持续增长。
 *
 * <p>这里把"检查冷却"和"记录本次提交"合并为一次 {@code SET NX EX}，原因是二者原本是
 * 分开的两步，中间存在竞态：同一个学生的并发请求可能都通过检查再各自记录。合并后由
 * Redis 的原子性保证同一次冷却期内只有一个请求能成功。
 *
 * <h2>降级策略</h2>
 * <p>冷却用于防抖，不是安全边界，且下游 Judge0 侧还有真实资源约束。Redis 不可用时放行
 * （fail-open）并告警，避免一次 Redis 抖动就让所有学生无法提交代码。
 */
@Component
public class SubmitCooldownStore {

    private final RedisStateStore store;

    public SubmitCooldownStore(RedisStateStore store) {
        this.store = store;
    }

    /**
     * 尝试占用一次提交额度。
     *
     * @param cooldown 冷却时长
     * @return 零或负值表示已放行（并已开始计时）；正值表示需要等待的剩余时长
     */
    public Duration acquire(Long studentId, Long problemId, Duration cooldown) {
        StringRedisTemplate redis = store.template();
        String key = RedisKeys.submitCooldown(studentId, problemId);

        Boolean acquired = store.withRedis(
                "提交冷却",
                () -> redis.opsForValue().setIfAbsent(key, "1", cooldown),
                null
        );

        if (acquired == null) {
            // Redis 不可用：放行，日志已由 RedisStateStore 记录
            return Duration.ZERO;
        }
        if (Boolean.TRUE.equals(acquired)) {
            return Duration.ZERO;
        }

        Long remainingSeconds = store.withRedis(
                "提交冷却剩余时间",
                () -> redis.getExpire(key, java.util.concurrent.TimeUnit.SECONDS),
                null
        );
        if (remainingSeconds == null || remainingSeconds <= 0) {
            // 键在两次调用之间刚好过期，或 TTL 查询失败：放行，不因查询问题误拒学生提交
            return Duration.ZERO;
        }
        return Duration.ofSeconds(remainingSeconds);
    }
}
