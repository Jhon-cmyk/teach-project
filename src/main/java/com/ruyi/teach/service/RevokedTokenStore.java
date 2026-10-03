package com.ruyi.teach.service;

/**
 * 已登出 Token 的存放位置。
 *
 * <p>抽成接口是为了让 {@code TabAuthTokenService} 不必直接依赖 Redis：
 * 单元测试可以注入内存实现，从而在没有 Redis 的情况下验证"只吊销当前标签页 Token"的行为。
 */
public interface RevokedTokenStore {

    /**
     * 判断 Token 是否已被吊销。
     *
     * @param tokenHash     Token 的 SHA-256 指纹
     * @param tokenExpiresAt Token 自身的过期时间戳（毫秒）
     * @return true 表示该 Token 不应再被接受
     */
    boolean isRevoked(String tokenHash, long tokenExpiresAt);

    /**
     * 将 Token 加入吊销名单。
     *
     * @param tokenHash      Token 的 SHA-256 指纹
     * @param tokenExpiresAt Token 自身的过期时间戳（毫秒），用于决定名单条目的存活时间
     */
    void revoke(String tokenHash, long tokenExpiresAt);
}
