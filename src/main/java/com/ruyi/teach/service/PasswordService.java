package com.ruyi.teach.service;

/**
 * 密码存储和校验的唯一入口。
 */
public interface PasswordService {

    /**
     * 将明文密码编码为可持久化的 BCrypt 哈希。
     */
    String encode(String rawPassword);

    /**
     * 校验明文密码，兼容 BCrypt 和历史固定盐 MD5。
     */
    boolean matches(String rawPassword, String storedPassword);

    /**
     * 判断一次成功校验后是否应把历史哈希升级为当前算法。
     */
    boolean needsUpgrade(String storedPassword);
}
