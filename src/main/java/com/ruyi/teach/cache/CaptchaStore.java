package com.ruyi.teach.cache;

import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

/**
 * 图形验证码的跨实例存储与浏览器绑定。
 *
 * <h2>答案存 Redis</h2>
 * 保证"A 实例取码、请求落到 B 实例"时依然能校验通过；校验使用 {@code GETDEL}，
 * 同一个验证码只能被消费一次。
 *
 * <h2>绑定用签名 Cookie，不用 Session</h2>
 * 验证码还需要回答"这个码是不是当前浏览器申请的"，否则验证码会退化成可重放的口令。
 * 最初这里用的是 {@code HttpSession}，但那是个<b>设计错误</b>：Session 是实例私有的内存，
 * A 写入的绑定 B 读不到，等于把"验证码跨实例可用"这个目的直接废掉。
 *
 * <p>改为把 {@code captchaId + 过期时间} 做 HMAC 签名后放进浏览器 Cookie：
 * 任何实例都能独立验签，不依赖共享存储，也不依赖 Session。
 * 安全性上把验证码限定在"同一个浏览器"范围内，与原先的 Session 绑定等价。
 *
 * <p>签名密钥默认复用 {@code auth.token.secret}（多实例必须配置一致，否则绑定失效）；
 * 也可用 {@code captcha.binding-secret} 单独指定。
 */
@Component
public class CaptchaStore {

    /** 浏览器绑定 Cookie 的名称。 */
    public static final String BINDING_COOKIE = "captcha_binding";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RedisStateStore store;

    /** 验证码有效期，与前端刷新提示保持一致。 */
    private Duration ttl = Duration.ofMinutes(5);

    /** 绑定签名密钥；多实例必须一致。 */
    private byte[] bindingKey = "__local-development-only__".getBytes(StandardCharsets.UTF_8);

    public CaptchaStore(RedisStateStore store) {
        this.store = store;
    }

    @Value("${captcha.ttl:5m}")
    void configureTtl(Duration configuredTtl) {
        if (configuredTtl == null || configuredTtl.isZero() || configuredTtl.isNegative()) {
            throw new IllegalArgumentException("Captcha TTL must be positive");
        }
        this.ttl = configuredTtl;
    }

    @Value("${captcha.binding-secret:${auth.token.secret:__local-development-only__}}")
    void configureBindingSecret(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            return;
        }
        this.bindingKey = sha256(configuredSecret.trim());
    }

    public Duration ttl() {
        return ttl;
    }

    /**
     * 写入验证码答案。
     *
     * @return 成功时返回验证码 ID，Redis 不可用时返回 null
     */
    public String create(String code) {
        String captchaId = IdUtil.simpleUUID();
        Boolean stored = store.withRedis(
                "验证码写入",
                () -> {
                    store.template().opsForValue()
                            .set(RedisKeys.captcha(captchaId), code, ttl);
                    return Boolean.TRUE;
                },
                Boolean.FALSE
        );
        return Boolean.TRUE.equals(stored) ? captchaId : null;
    }

    /**
     * 取出并删除验证码答案（一次性消费）。
     *
     * @return 验证码答案；不存在、已过期或 Redis 不可用时均返回 null
     */
    public String consume(String captchaId) {
        StringRedisTemplate redis = store.template();
        return store.withRedis(
                "验证码校验",
                () -> redis.opsForValue().getAndDelete(RedisKeys.captcha(captchaId)),
                null
        );
    }

    /** 为验证码签发浏览器绑定令牌，由调用方写入 Cookie。 */
    public String issueBinding(String captchaId) {
        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        return expiresAt + "." + sign(captchaId, expiresAt);
    }

    /** 校验绑定令牌是否由本服务签发、未过期，且对应当前 captchaId。 */
    public boolean verifyBinding(String captchaId, String bindingToken) {
        if (captchaId == null || bindingToken == null || bindingToken.isBlank()) {
            return false;
        }
        String[] parts = bindingToken.split("\\.", 2);
        if (parts.length != 2) {
            return false;
        }
        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            return false;
        }
        return MessageDigest.isEqual(
                sign(captchaId, expiresAt).getBytes(StandardCharsets.US_ASCII),
                parts[1].getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String sign(String captchaId, long expiresAt) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(bindingKey, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((captchaId + ":" + expiresAt).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign captcha binding", e);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
