package com.ruyi.teach.config;

import com.ruyi.teach.controller.SessionUserContext;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.enums.UserRole;
import com.ruyi.teach.service.RoleAuthorizationService;
import com.ruyi.teach.service.TabAuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Set;

@Component
public class SessionAuthenticationInterceptor implements HandlerInterceptor {

    private final RoleAuthorizationService roleAuthorizationService;
    private final TabAuthTokenService tabAuthTokenService;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/user/register",
            "/user/login/captcha",
            "/user/captcha",
            "/course/list/all",
            "/chapter/list",
            "/doc.html",
            "/swagger-ui.html",
            "/error"
    );

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/profile/",
            "/webjars/",
            "/v3/api-docs",
            "/swagger-ui/"
    );

    private static final List<RolePathRule> ROLE_PATH_RULES = List.of(
            new RolePathRule("/admin", UserRole.ADMIN),
            new RolePathRule("/student", UserRole.STUDENT),
            new RolePathRule("/teacher", UserRole.TEACHER, UserRole.ADMIN),
            new RolePathRule("/ai/agent", UserRole.TEACHER, UserRole.ADMIN),
            new RolePathRule("/ai/resource", UserRole.TEACHER, UserRole.ADMIN),
            new RolePathRule("/ai/micro-video", UserRole.TEACHER),
            new RolePathRule("/analysis", UserRole.TEACHER, UserRole.ADMIN),
            new RolePathRule("/coding/problem/teacher", UserRole.TEACHER),
            new RolePathRule("/coding/problem/student", UserRole.STUDENT),
            new RolePathRule("/homework/teacher", UserRole.TEACHER),
            new RolePathRule("/homework/student", UserRole.STUDENT),
            new RolePathRule("/course-analysis", UserRole.STUDENT)
    );

    @Autowired
    public SessionAuthenticationInterceptor(RoleAuthorizationService roleAuthorizationService,
                                            TabAuthTokenService tabAuthTokenService) {
        this.roleAuthorizationService = roleAuthorizationService;
        this.tabAuthTokenService = tabAuthTokenService;
    }

    public SessionAuthenticationInterceptor(RoleAuthorizationService roleAuthorizationService) {
        this(roleAuthorizationService, null);
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(request)) {
            return true;
        }

        if (tabAuthTokenService != null) {
            String bearerToken = tabAuthTokenService.extractBearer(request);
            if (bearerToken != null) {
                User tokenUser = tabAuthTokenService.resolve(bearerToken);
                if (tokenUser == null) {
                    throw new BusinessException(
                            ErrorCode.NOT_LOGIN_ERROR,
                            "登录凭证已失效，请重新登录"
                    );
                }
                SessionUserContext.bindRequest(request, tokenUser);
            }
        }

        User loginUser = SessionUserContext.require(request);
        String path = getApplicationPath(request);
        for (RolePathRule rule : ROLE_PATH_RULES) {
            if (rule.matches(path)) {
                roleAuthorizationService.requireAnyRole(loginUser, rule.allowedRoles());
                break;
            }
        }
        return true;
    }

    boolean isPublicPath(HttpServletRequest request) {
        String path = getApplicationPath(request);
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String getApplicationPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private record RolePathRule(String pathPrefix, UserRole... allowedRoles) {
        private boolean matches(String path) {
            return path.equals(pathPrefix) || path.startsWith(pathPrefix + "/");
        }
    }
}
