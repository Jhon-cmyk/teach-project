package com.ruyi.teach.controller;

import com.ruyi.teach.config.SessionAuthenticationInterceptor;
import com.ruyi.teach.config.TraceIdFilter;
import com.ruyi.teach.exception.GlobalExceptionHandler;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.CaptchaVO;
import com.ruyi.teach.service.UserService;
import com.ruyi.teach.service.RoleAuthorizationService;
import com.ruyi.teach.service.TabAuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionAuthenticationWebTest {

    private MockMvc mockMvc;
    private UserService userService;
    private TabAuthTokenService tabAuthTokenService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        tabAuthTokenService = mock(TabAuthTokenService.class);
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "tabAuthTokenService", tabAuthTokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new SessionAuthenticationInterceptor(new RoleAuthorizationService()))
                .addFilters(new TraceIdFilter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void captchaEndpointRemainsAnonymous() throws Exception {
        when(userService.generateCaptcha(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CaptchaVO());

        mockMvc.perform(get("/user/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void protectedEndpointReturnsUnifiedNotLoginResponse() throws Exception {
        mockMvc.perform(get("/user/get/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.message").value("登录状态已失效，请重新登录"));
    }

    @Test
    void authenticatedSessionCanAccessProtectedEndpointAndLogout() throws Exception {
        User sessionUser = user(7L);
        User databaseUser = user(7L);
        databaseUser.setUserName("测试用户");
        when(userService.getById(7L)).thenReturn(databaseUser);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUserContext.SESSION_USER_KEY, sessionUser);

        mockMvc.perform(get("/user/get/login").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(7));

        mockMvc.perform(post("/user/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void invalidRegistrationReturnsFieldLevelValidationErrors() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "userAccount": "a",
                                  "userPassword": "123",
                                  "checkPassword": "456",
                                  "userRole": "root"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("请求参数校验失败"))
                .andExpect(jsonPath("$.data.userAccount").exists())
                .andExpect(jsonPath("$.data.userPassword").exists())
                .andExpect(jsonPath("$.data.checkPassword").value("两次输入的密码不一致"))
                .andExpect(jsonPath("$.data.userRole").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void malformedJsonReturnsParamsErrorInsteadOfSystemError() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.data.request").exists());

        verifyNoInteractions(userService);
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUserAccount("student" + id);
        user.setUserRole("student");
        return user;
    }
}
