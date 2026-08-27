package com.ruyi.teach.service;

import com.ruyi.teach.mapper.UserMapper;
import com.ruyi.teach.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TabAuthTokenServiceTest {

    private TabAuthTokenService service;
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        service = new TabAuthTokenService();
        userMapper = mock(UserMapper.class);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
    }

    @Test
    void issuesSignedTokenThatResolvesOnlyWhenUntampered() {
        User user = new User();
        user.setId(12L);
        user.setUserRole("student");
        user.setUserPassword("should-not-leave-service");
        user.setIsDelete(0);
        when(userMapper.selectById(12L)).thenReturn(user);

        String rawToken = service.issue(user);

        assertFalse(rawToken.isBlank());
        assertNotNull(service.resolve(rawToken));
        assertNull(service.resolve(rawToken + "tampered"));
    }

    @Test
    void resolvesValidTokenToCurrentUserWithoutPassword() {
        User user = new User();
        user.setId(23L);
        user.setUserRole("teacher");
        user.setUserPassword("should-not-leave-service");
        user.setIsDelete(0);
        when(userMapper.selectById(23L)).thenReturn(user);
        String token = service.issue(user);

        User resolved = service.resolve(token);

        assertNotNull(resolved);
        assertEquals(23L, resolved.getId());
        assertEquals(null, resolved.getUserPassword());
    }

    @Test
    void logoutRevokesOnlyTheCurrentTabsToken() {
        User user = new User();
        user.setId(31L);
        user.setUserRole("student");
        user.setIsDelete(0);
        when(userMapper.selectById(31L)).thenReturn(user);
        String firstTabToken = service.issue(user);
        String secondTabToken = service.issue(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + firstTabToken);
        service.revoke(request);

        assertNull(service.resolve(firstTabToken));
        assertNotNull(service.resolve(secondTabToken));
    }

    @Test
    void defaultTokenValidityIsThirtyDays() {
        User user = new User();
        user.setId(41L);

        long issuedAt = System.currentTimeMillis();
        String token = service.issue(user);
        long expiresAt = readExpiresAt(token);

        long expectedTtl = Duration.ofDays(30).toMillis();
        long actualTtl = expiresAt - issuedAt;
        assertTrue(Math.abs(expectedTtl - actualTtl) <= 2_000L);
    }

    @Test
    void configuredSecretKeepsTokensValidAcrossServiceRestart() {
        TabAuthTokenService firstInstance = new TabAuthTokenService();
        TabAuthTokenService restartedInstance = new TabAuthTokenService();
        firstInstance.configureSigningSecret("stable-test-secret");
        restartedInstance.configureSigningSecret("stable-test-secret");
        ReflectionTestUtils.setField(firstInstance, "userMapper", userMapper);
        ReflectionTestUtils.setField(restartedInstance, "userMapper", userMapper);

        User user = new User();
        user.setId(51L);
        user.setIsDelete(0);
        when(userMapper.selectById(51L)).thenReturn(user);

        String token = firstInstance.issue(user);

        assertNotNull(restartedInstance.resolve(token));
    }

    private long readExpiresAt(String token) {
        String encodedPayload = token.split("\\.", 2)[0];
        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        return Long.parseLong(payload.split(":", 3)[1]);
    }
}
