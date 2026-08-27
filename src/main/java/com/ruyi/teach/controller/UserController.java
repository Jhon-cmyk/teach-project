package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.CaptchaLoginRequest;
import com.ruyi.teach.model.dto.UserRegisterRequest;
import com.ruyi.teach.model.dto.UserUpdatePasswordRequest;
import com.ruyi.teach.model.dto.UserUpdateRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.CaptchaVO;
import com.ruyi.teach.service.UserService;
import com.ruyi.teach.service.TabAuthTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理")
public class UserController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private UserService userService;

    @Resource
    private TabAuthTokenService tabAuthTokenService;

    @Operation(summary = "账号密码 + 图形验证码登录")
    @PostMapping("/login/captcha")
    public BaseResponse<User> userLoginByCaptcha(@Valid @RequestBody CaptchaLoginRequest loginRequest,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        User user = userService.userLoginByCaptcha(loginRequest, request);
        response.setHeader(TabAuthTokenService.RESPONSE_HEADER, tabAuthTokenService.issue(user));
        return ResultUtils.success(user);
    }

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public BaseResponse<CaptchaVO> getCaptcha(HttpServletRequest request) {
        return ResultUtils.success(userService.generateCaptcha(request));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@Valid @RequestBody UserRegisterRequest userRegisterRequest) {

        String userRole = userRegisterRequest.getUserRole();
        if (userRole == null || userRole.trim().isEmpty()) {
            userRole = "student";
        }
        if (!"teacher".equals(userRole)) {
            userRole = "student";
        }

        long result = userService.userRegister(
                userRegisterRequest.getUserAccount(),
                userRegisterRequest.getUserPassword(),
                userRegisterRequest.getCheckPassword(),
                userRegisterRequest.getUserName(),
                userRole,
                userRegisterRequest.getTeacherRegisterCode()
        );
        return ResultUtils.success(result);
    }

    @Operation(summary = "获取当前登录用户详情")
    @GetMapping("/get/login")
    public BaseResponse<User> getLoginUser(HttpServletRequest request) {
        User currentUser = SessionUserContext.require(request);
        User user = userService.getById(currentUser.getId());
        if (user != null) {
            user.setUserPassword(null);
        }
        return ResultUtils.success(user);
    }

    @Operation(summary = "更新个人信息")
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyInfo(@Valid @RequestBody UserUpdateRequest updateRequest, HttpServletRequest request) {

        User currentUser = SessionUserContext.require(request);

        User existingUser = userService.getById(currentUser.getId());
        if (existingUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        User user = new User();
        user.setId(currentUser.getId());
        user.setUserName(updateRequest.getUserName());
        user.setUserProfile(resolveEditableUserProfile(updateRequest.getUserProfile(), existingUser));
        user.setUserAvatar(updateRequest.getUserAvatar());

        boolean result = userService.updateById(user);
        return ResultUtils.success(result);
    }

    private String resolveEditableUserProfile(String requestedProfile, User existingUser) {
        if (!"teacher".equals(existingUser.getUserRole())) {
            return requestedProfile;
        }

        JsonNode existingProfile = parseProfileJson(existingUser.getUserProfile());
        JsonNode requestedProfileJson = parseProfileJson(requestedProfile);
        ObjectNode mergedProfile = OBJECT_MAPPER.createObjectNode();

        if (existingProfile != null && existingProfile.isObject()) {
            existingProfile.fields().forEachRemaining(entry -> mergedProfile.set(entry.getKey(), entry.getValue()));
        }

        String existingDepartment = readText(existingProfile, "department");
        String existingTitle = firstNonBlank(existingUser.getTeacherTitle(), readText(existingProfile, "title"));
        String requestedBio = readText(requestedProfileJson, "bio");

        mergedProfile.put("department", existingDepartment);
        mergedProfile.put("title", existingTitle);
        mergedProfile.put("bio", requestedBio);
        return mergedProfile.toString();
    }

    private JsonNode parseProfileJson(String profile) {
        if (profile == null || profile.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(profile);
            return node != null && node.isObject() ? node : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return "";
        }
        return node.get(fieldName).asText("");
    }

    private String firstNonBlank(String first, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    @Operation(summary = "修改密码")
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updatePassword(@Valid @RequestBody UserUpdatePasswordRequest pwdRequest, HttpServletRequest request) {

        User currentUser = SessionUserContext.require(request);

        boolean result = userService.updateUserPassword(
                currentUser.getId(),
                pwdRequest.getOldPassword(),
                pwdRequest.getNewPassword()
        );
        return ResultUtils.success(result);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        tabAuthTokenService.revoke(request);
        SessionUserContext.logout(request);
        return ResultUtils.success(true);
    }

}
