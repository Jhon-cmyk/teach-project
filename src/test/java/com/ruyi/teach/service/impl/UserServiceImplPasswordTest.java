package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class UserServiceImplPasswordTest {

    private PasswordServiceImpl passwordService;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordServiceImpl(new BCryptPasswordEncoder());
        userService = spy(new UserServiceImpl());
        ReflectionTestUtils.setField(userService, "passwordService", passwordService);
    }

    @Test
    void registersNewUserWithBcryptPassword() {
        doReturn(0L).when(userService).count(any(Wrapper.class));
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return true;
        }).when(userService).save(any(User.class));

        long userId = userService.userRegister(
                "student001",
                "new-password",
                "new-password",
                "测试用户",
                "student",
                null
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        assertTrue(passwordService.matches("new-password", userCaptor.getValue().getUserPassword()));
        assertFalse(passwordService.needsUpgrade(userCaptor.getValue().getUserPassword()));
        assertTrue(userId == 42L);
    }

    @Test
    void upgradesLegacyMd5AfterSuccessfulLogin() {
        User legacyUser = user(7L, "teacher001", legacyHash("old-password"));
        doReturn(legacyUser).when(userService).getOne(any(Wrapper.class));
        doReturn(true).when(userService).updateById(any(User.class));
        MockHttpServletRequest request = new MockHttpServletRequest();

        User result = userService.userLogin("teacher001", "old-password", request);

        ArgumentCaptor<User> updateCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(updateCaptor.capture());
        User passwordUpdate = updateCaptor.getValue();
        assertTrue(passwordService.matches("old-password", passwordUpdate.getUserPassword()));
        assertFalse(passwordService.needsUpgrade(passwordUpdate.getUserPassword()));
        assertNull(result.getUserPassword());
        assertNotNull(request.getSession().getAttribute("user_login"));
    }

    @Test
    void doesNotRewriteCurrentBcryptAfterLogin() {
        User currentUser = user(8L, "teacher002", passwordService.encode("current-password"));
        doReturn(currentUser).when(userService).getOne(any(Wrapper.class));

        userService.userLogin("teacher002", "current-password", new MockHttpServletRequest());

        verify(userService, never()).updateById(any(User.class));
    }

    @Test
    void rejectsWrongPassword() {
        User user = user(9L, "teacher003", passwordService.encode("correct-password"));
        doReturn(user).when(userService).getOne(any(Wrapper.class));

        assertThrows(
                BusinessException.class,
                () -> userService.userLogin("teacher003", "wrong-password", new MockHttpServletRequest())
        );
        verify(userService, never()).updateById(any(User.class));
    }

    @Test
    void changesPasswordToBcryptAfterCheckingLegacyPassword() {
        User user = user(10L, "teacher004", legacyHash("old-password"));
        doReturn(user).when(userService).getById(10L);
        doReturn(true).when(userService).updateById(any(User.class));

        assertTrue(userService.updateUserPassword(10L, "old-password", "new-password"));

        ArgumentCaptor<User> updateCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(updateCaptor.capture());
        assertTrue(passwordService.matches("new-password", updateCaptor.getValue().getUserPassword()));
        assertFalse(passwordService.needsUpgrade(updateCaptor.getValue().getUserPassword()));
    }

    private User user(long id, String account, String password) {
        User user = new User();
        user.setId(id);
        user.setUserAccount(account);
        user.setUserPassword(password);
        user.setUserName("测试教师");
        user.setUserRole("teacher");
        return user;
    }

    private String legacyHash(String rawPassword) {
        return DigestUtils.md5DigestAsHex(
                ("ruyi_teach" + rawPassword).getBytes(StandardCharsets.UTF_8)
        );
    }
}
