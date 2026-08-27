package com.ruyi.teach.service;

import com.ruyi.teach.mapper.UserMapper;
import com.ruyi.teach.model.entity.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TabAuthTokenService {

    public static final String RESPONSE_HEADER = "X-Auth-Token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration DEFAULT_TOKEN_TTL = Duration.ofDays(30);

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecureRandom secureRandom = new SecureRandom();
    private Duration tokenTtl = DEFAULT_TOKEN_TTL;
    private byte[] signingKey = randomBytes(32);
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    @Resource
    private UserMapper userMapper;

    @Value("${auth.token.ttl:30d}")
    void configureTokenTtl(Duration configuredTtl) {
        if (configuredTtl == null || configuredTtl.isZero() || configuredTtl.isNegative()) {
            throw new IllegalArgumentException("Authentication token TTL must be positive");
        }
        this.tokenTtl = configuredTtl;
    }

    @Value("${auth.token.secret:}")
    void configureSigningSecret(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            return;
        }
        this.signingKey = sha256(configuredSecret.trim());
    }

    public String issue(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("login user and id must not be null");
        }

        cleanupRevokedTokens();
        long expiresAt = System.currentTimeMillis() + tokenTtl.toMillis();
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(18));
        String payload = user.getId() + ":" + expiresAt + ":" + nonce;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    public User resolve(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        if (revokedTokens.containsKey(hash(rawToken))) {
            return null;
        }

        String[] tokenParts = rawToken.split("\\.", 2);
        if (tokenParts.length != 2 || !constantTimeEquals(sign(tokenParts[0]), tokenParts[1])) {
            return null;
        }

        String[] payloadParts;
        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(tokenParts[0]),
                    StandardCharsets.UTF_8
            );
            payloadParts = payload.split(":", 3);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (payloadParts.length != 3) {
            return null;
        }

        long userId;
        long expiresAt;
        try {
            userId = Long.parseLong(payloadParts[0]);
            expiresAt = Long.parseLong(payloadParts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (userId <= 0 || expiresAt <= System.currentTimeMillis()) {
            return null;
        }

        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            return null;
        }
        user.setUserPassword(null);
        return user;
    }

    public void revoke(HttpServletRequest request) {
        String rawToken = extractBearer(request);
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        Long expiresAt = readExpiresAt(rawToken);
        if (expiresAt != null && expiresAt > System.currentTimeMillis()) {
            revokedTokens.put(hash(rawToken), expiresAt);
        }
    }

    public String extractBearer(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign authentication token", e);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private Long readExpiresAt(String rawToken) {
        try {
            String[] tokenParts = rawToken.split("\\.", 2);
            if (tokenParts.length != 2 || !constantTimeEquals(sign(tokenParts[0]), tokenParts[1])) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[0]), StandardCharsets.UTF_8);
            String[] payloadParts = payload.split(":", 3);
            return payloadParts.length == 3 ? Long.parseLong(payloadParts[1]) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void cleanupRevokedTokens() {
        long now = System.currentTimeMillis();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    static String hash(String rawToken) {
        return HexFormat.of().formatHex(sha256(rawToken));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
