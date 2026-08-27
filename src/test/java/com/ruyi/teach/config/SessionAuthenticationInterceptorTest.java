package com.ruyi.teach.config;

import com.ruyi.teach.controller.SessionUserContext;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.RoleAuthorizationService;
import com.ruyi.teach.service.TabAuthTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionAuthenticationInterceptorTest {

    private final SessionAuthenticationInterceptor interceptor =
            new SessionAuthenticationInterceptor(new RoleAuthorizationService());

    @Test
    void permitsDocumentedAnonymousEndpoints() {
        assertTrue(preHandle("GET", "/api/user/captcha"));
        assertTrue(preHandle("POST", "/api/user/login/captcha"));
        assertTrue(preHandle("POST", "/api/user/register"));
        assertTrue(preHandle("GET", "/api/course/list/all"));
        assertTrue(preHandle("GET", "/api/chapter/list"));
        assertTrue(preHandle("GET", "/api/profile/avatar.png"));
        assertTrue(preHandle("GET", "/api/v3/api-docs"));
        assertTrue(preHandle("OPTIONS", "/api/course/list"));
    }

    @Test
    void rejectsAnonymousAccessToBusinessEndpoints() {
        MockHttpServletRequest request = request("GET", "/api/course/list");

        assertThrows(
                BusinessException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void permitsAuthenticatedAccessToBusinessEndpoints() {
        MockHttpServletRequest request = request("GET", "/api/course/list");
        User user = new User();
        user.setId(1L);
        user.setUserRole("student");
        SessionUserContext.login(request, user);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void enforcesRoleRulesForProtectedNamespaces() {
        assertTrue(preHandleAs("GET", "/api/admin/dashboard", "admin"));
        assertTrue(preHandleAs("GET", "/api/teacher/dashboard", "teacher"));
        assertTrue(preHandleAs("GET", "/api/teacher/course-graph", "admin"));
        assertTrue(preHandleAs("GET", "/api/student/learning-profile", "student"));

        BusinessException adminFailure = assertThrows(
                BusinessException.class,
                () -> preHandleAs("GET", "/api/admin/dashboard", "teacher")
        );
        assertEquals(40101, adminFailure.getCode());

        assertThrows(
                BusinessException.class,
                () -> preHandleAs("GET", "/api/student/learning-profile", "teacher")
        );
        assertThrows(
                BusinessException.class,
                () -> preHandleAs("GET", "/api/coding/problem/teacher/list", "student")
        );
    }

    @Test
    void bearerTokenIdentityOverridesSharedBrowserSession() {
        TabAuthTokenService tokenService = mock(TabAuthTokenService.class);
        SessionAuthenticationInterceptor tokenInterceptor =
                new SessionAuthenticationInterceptor(new RoleAuthorizationService(), tokenService);
        MockHttpServletRequest request = request("GET", "/api/teacher/dashboard");

        User sharedSessionStudent = new User();
        sharedSessionStudent.setId(10L);
        sharedSessionStudent.setUserRole("student");
        SessionUserContext.login(request, sharedSessionStudent);

        User tabTeacher = new User();
        tabTeacher.setId(20L);
        tabTeacher.setUserRole("teacher");
        request.addHeader("Authorization", "Bearer teacher-tab-token");
        when(tokenService.extractBearer(request)).thenReturn("teacher-tab-token");
        when(tokenService.resolve("teacher-tab-token")).thenReturn(tabTeacher);

        assertTrue(tokenInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(20L, SessionUserContext.require(request).getId());
    }

    @Test
    void invalidBearerTokenNeverFallsBackToSharedBrowserSession() {
        TabAuthTokenService tokenService = mock(TabAuthTokenService.class);
        SessionAuthenticationInterceptor tokenInterceptor =
                new SessionAuthenticationInterceptor(new RoleAuthorizationService(), tokenService);
        MockHttpServletRequest request = request("GET", "/api/student/dashboard");

        User sharedSessionStudent = new User();
        sharedSessionStudent.setId(10L);
        sharedSessionStudent.setUserRole("student");
        SessionUserContext.login(request, sharedSessionStudent);
        request.addHeader("Authorization", "Bearer expired-tab-token");
        when(tokenService.extractBearer(request)).thenReturn("expired-tab-token");
        when(tokenService.resolve("expired-tab-token")).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> tokenInterceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
        assertEquals(40100, exception.getCode());
    }

    private boolean preHandle(String method, String path) {
        MockHttpServletRequest request = request(method, path);
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setContextPath("/api");
        return request;
    }

    private boolean preHandleAs(String method, String path, String role) {
        MockHttpServletRequest request = request(method, path);
        User user = new User();
        user.setId(1L);
        user.setUserRole(role);
        SessionUserContext.login(request, user);
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }
}
