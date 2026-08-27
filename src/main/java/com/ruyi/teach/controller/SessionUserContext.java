package com.ruyi.teach.controller;

import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Session 登录用户的唯一读写入口。
 */
public final class SessionUserContext {

    public static final String SESSION_USER_KEY = "user_login";
    private static final String REQUEST_USER_KEY =
            SessionUserContext.class.getName() + ".currentUser";

    private SessionUserContext() {
    }

    public static User getOptional(HttpServletRequest request) {
        Object requestUser = request.getAttribute(REQUEST_USER_KEY);
        if (requestUser instanceof User user && user.getId() != null) {
            return user;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object sessionUser = session.getAttribute(SESSION_USER_KEY);
        if (!(sessionUser instanceof User user) || user.getId() == null) {
            return null;
        }

        request.setAttribute(REQUEST_USER_KEY, user);
        return user;
    }

    public static User require(HttpServletRequest request) {
        User user = getOptional(request);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态已失效，请重新登录");
        }
        return user;
    }

    /**
     * 将已通过独立 Token 验证的用户绑定到本次请求。请求级身份优先于共享 Session。
     */
    public static void bindRequest(HttpServletRequest request, User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("request user and id must not be null");
        }
        request.setAttribute(REQUEST_USER_KEY, user);
    }

    public static void login(HttpServletRequest request, User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("login user and id must not be null");
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        } else {
            request.changeSessionId();
        }
        session.setAttribute(SESSION_USER_KEY, user);
        request.setAttribute(REQUEST_USER_KEY, user);
    }

    public static void replace(HttpServletRequest request, User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("session user and id must not be null");
        }
        request.getSession(true).setAttribute(SESSION_USER_KEY, user);
        request.setAttribute(REQUEST_USER_KEY, user);
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        request.removeAttribute(REQUEST_USER_KEY);
    }
}
