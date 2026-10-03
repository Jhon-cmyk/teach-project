package com.ruyi.teach.cache;

/**
 * Redis 键的唯一出口。
 *
 * <p>集中定义避免同一份状态在不同类里被写成不同前缀，也便于运维按前缀排查与清理
 * （测试清理同样依赖 {@link #KEY_PREFIX}）。
 *
 * <p>这里只存放"必须被所有实例看到"的短生命周期状态。业务主数据（课程、章节、
 * 题目、作业等）不进 Redis，MySQL 始终是权威源。
 */
public final class RedisKeys {

    /** 统一前缀：多项目共用一个 Redis 实例时用于区分归属。 */
    public static final String KEY_PREFIX = "teach:";

    private static final String CAPTCHA_PREFIX = KEY_PREFIX + "captcha:";
    private static final String REVOKED_TOKEN_PREFIX = KEY_PREFIX + "auth:revoked:";
    private static final String AI_RATE_LIMIT_PREFIX = KEY_PREFIX + "ratelimit:ai:";
    private static final String SUBMIT_COOLDOWN_PREFIX = KEY_PREFIX + "cooldown:submit:";

    private RedisKeys() {
    }

    /** 图形验证码答案，TTL 5 分钟，校验时用 GETDEL 一次性消费。 */
    public static String captcha(String captchaId) {
        return CAPTCHA_PREFIX + captchaId;
    }

    /** 已登出 Token 的 SHA-256 指纹，TTL 跟随该 Token 的剩余有效期。 */
    public static String revokedToken(String tokenHash) {
        return REVOKED_TOKEN_PREFIX + tokenHash;
    }

    /** AI 接口按用户的滑动窗口计数（ZSET，score/member 均为毫秒时间戳）。 */
    public static String aiRateLimit(Long userId) {
        return AI_RATE_LIMIT_PREFIX + userId;
    }

    /** 编程题提交冷却标记。 */
    public static String submitCooldown(Long studentId, Long problemId) {
        return SUBMIT_COOLDOWN_PREFIX + studentId + ":" + problemId;
    }
}
