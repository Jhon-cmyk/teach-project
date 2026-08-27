package com.ruyi.teach.controller;

import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionUserContextTest {

    @Test
    void anonymousRequestHasNoCurrentUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertNull(SessionUserContext.getOptional(request));
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> SessionUserContext.require(request)
        );
        assertEquals(40100, exception.getCode());
        assertNull(request.getSession(false));
    }

    @Test
    void loginStoresUserAndLogoutInvalidatesSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        User user = user(7L);

        SessionUserContext.login(request, user);

        assertSame(user, SessionUserContext.require(request));
        assertSame(user, request.getSession(false).getAttribute(SessionUserContext.SESSION_USER_KEY));

        SessionUserContext.logout(request);

        assertNull(SessionUserContext.getOptional(request));
        assertTrue(request.getSession(false) == null || request.getSession(false).isNew());
    }

    @Test
    void loginRotatesExistingSessionIdAndPreservesCaptchaState() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("captcha:test", "1234");
        String oldSessionId = request.getSession(false).getId();

        SessionUserContext.login(request, user(9L));

        assertNotEquals(oldSessionId, request.getSession(false).getId());
        assertEquals("1234", request.getSession(false).getAttribute("captcha:test"));
        assertEquals(9L, SessionUserContext.require(request).getId());
    }

    @Test
    void replaceRefreshesSessionUserWithoutCreatingAnotherIdentity() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SessionUserContext.login(request, user(8L));
        User refreshed = user(8L);
        refreshed.setPoints(20);

        SessionUserContext.replace(request, refreshed);

        assertSame(refreshed, SessionUserContext.require(request));
        assertEquals(20, SessionUserContext.require(request).getPoints());
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUserAccount("user" + id);
        user.setUserRole("student");
        return user;
    }
}
