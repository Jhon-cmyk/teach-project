package com.ruyi.teach.common;

/**
 * 便于确认运行中的实例到底是哪一版代码。
 *
 * <p>排查"改了代码但行为没变"时，先看这个标记比翻日志快：
 * 验证码绑定从 Session 改为签名 Cookie 的那次修复即靠它区分新旧构建。
 */
public final class BuildStamp {

    /** 每次需要区分新旧行为时更新这个值，并可通过 /user/captcha 响应头观察。 */
    public static final String VALUE = "captcha-binding-cookie-v2";

    private BuildStamp() {
    }
}
