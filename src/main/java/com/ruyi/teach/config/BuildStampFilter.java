package com.ruyi.teach.config;

import com.ruyi.teach.common.BuildStamp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 在每个响应上暴露构建标记。
 *
 * <p>用途是排查"改了代码但行为没变"这类问题：例如验证码绑定由 Session 改为签名 Cookie 后，
 * 若某个实例没有重启，仅看接口报错很难判断跑的是哪一版代码。带上这个响应头即可一眼确认。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class BuildStampFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Build-Stamp";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader(HEADER_NAME, BuildStamp.VALUE);
        filterChain.doFilter(request, response);
    }
}
