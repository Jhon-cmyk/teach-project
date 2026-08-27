package com.ruyi.teach.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.exception.ThrowUtils;
import com.ruyi.teach.controller.SessionUserContext;
import com.ruyi.teach.mapper.TeacherRegistrationCodeMapper;
import com.ruyi.teach.mapper.UserLoginLogMapper;
import com.ruyi.teach.mapper.UserMapper;
import com.ruyi.teach.model.dto.CaptchaLoginRequest;
import com.ruyi.teach.model.entity.TeacherRegistrationCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.UserLoginLog;
import com.ruyi.teach.model.vo.CaptchaVO;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.PasswordService;
import com.ruyi.teach.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Date;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String CAPTCHA_SESSION_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL_MS = 5 * 60 * 1000L;

    @Resource
    private UserLoginLogMapper userLoginLogMapper;

    @Resource
    private TeacherRegistrationCodeMapper teacherRegistrationCodeMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Resource
    private PasswordService passwordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        ThrowUtils.throwIf(StringUtils.isAnyBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "账号或密码为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度过短");

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.getOne(queryWrapper);

        ThrowUtils.throwIf(
                user == null || !passwordService.matches(userPassword, user.getUserPassword()),
                ErrorCode.PARAMS_ERROR,
                "账号或密码错误"
        );
        upgradePasswordIfNeeded(user, userPassword);

        User safetyUser = getSafetyUser(user);
        SessionUserContext.login(request, safetyUser);
        saveStudentLoginLog(user);
        if ("admin".equals(user.getUserRole())) {
            adminAuditLogger.log(safetyUser, "系统登录", "管理员登录", "user", user.getId(),
                    "账号=" + user.getUserAccount(), request);
        }

        return safetyUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(String userAccount, String userPassword, String checkPassword,
                             String userName, String userRole, String teacherRegisterCode) {
        ThrowUtils.throwIf(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号过短");
        ThrowUtils.throwIf(userPassword.length() < 6, ErrorCode.PARAMS_ERROR, "密码过短");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号已存在");

        boolean teacherRegister = "teacher".equals(userRole);
        TeacherRegistrationCode registrationCode = null;
        if (teacherRegister) {
            if (StringUtils.isBlank(teacherRegisterCode)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师注册需要填写管理端发放的注册号");
            }
            registrationCode = teacherRegistrationCodeMapper.selectOne(new QueryWrapper<TeacherRegistrationCode>()
                    .eq("register_code", teacherRegisterCode.trim())
                    .eq("status", "unused")
                    .eq("is_delete", 0));
            if (registrationCode == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师注册号无效或已被使用");
            }
        }

        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(passwordService.encode(userPassword));
        user.setUserName(StringUtils.isNotBlank(userName) ? userName : (teacherRegister ? "新教师" : "新同学"));
        user.setUserRole(teacherRegister ? "teacher" : "student");
        if (teacherRegister && registrationCode != null) {
            user.setTeacherTitle(registrationCode.getTeacherTitle());
            user.setTeacherRegisterCode(registrationCode.getRegisterCode());
        }

        boolean saveResult = this.save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败");

        if (teacherRegister && registrationCode != null) {
            TeacherRegistrationCode updateCode = new TeacherRegistrationCode();
            updateCode.setId(registrationCode.getId());
            updateCode.setStatus("used");
            updateCode.setUsedBy(user.getId());
            updateCode.setUsedTime(new Date());
            teacherRegistrationCodeMapper.updateById(updateCode);
        }

        return user.getId();
    }

    @Override
    public boolean updateUserPassword(Long id, String oldPassword, String newPassword) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        if (!passwordService.matches(oldPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能少于 6 位");
        }

        user.setUserPassword(passwordService.encode(newPassword));

        return this.updateById(user);
    }

    private void upgradePasswordIfNeeded(User user, String rawPassword) {
        if (!passwordService.needsUpgrade(user.getUserPassword())) {
            return;
        }

        User passwordUpdate = new User();
        passwordUpdate.setId(user.getId());
        passwordUpdate.setUserPassword(passwordService.encode(rawPassword));
        boolean updated = this.updateById(passwordUpdate);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "密码安全升级失败，请重试");
        user.setUserPassword(passwordUpdate.getUserPassword());
    }

    @Override
    public CaptchaVO generateCaptcha(HttpServletRequest request) {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 20);
        String code = captcha.getCode();
        String captchaId = IdUtil.simpleUUID();

        HttpSession session = request.getSession();
        session.setAttribute(CAPTCHA_SESSION_PREFIX + captchaId,
                new CaptchaEntry(code, System.currentTimeMillis() + CAPTCHA_TTL_MS));

        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setImageBase64(captcha.getImageBase64Data());
        if (isLocalRequest(request)) {
            vo.setCaptchaCode(code);
        }
        return vo;
    }

    @Override
    public User userLoginByCaptcha(CaptchaLoginRequest loginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(loginRequest == null, ErrorCode.PARAMS_ERROR);
        String captchaId = loginRequest.getCaptchaId();
        String captchaCode = loginRequest.getCaptchaCode();
        ThrowUtils.throwIf(StringUtils.isAnyBlank(captchaId, captchaCode), ErrorCode.PARAMS_ERROR, "请先完成图形验证");

        HttpSession session = request.getSession();
        String key = CAPTCHA_SESSION_PREFIX + captchaId;
        CaptchaEntry entry = (CaptchaEntry) session.getAttribute(key);
        if (entry == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已失效，请刷新");
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            session.removeAttribute(key);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期，请刷新");
        }
        if (!entry.code.equalsIgnoreCase(captchaCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        session.removeAttribute(key);

        return userLogin(loginRequest.getUserAccount(), loginRequest.getUserPassword(), request);
    }

    private void saveStudentLoginLog(User user) {
        if (user == null || user.getId() == null || !"student".equals(user.getUserRole())) {
            return;
        }

        try {
            Date now = new Date();
            UserLoginLog loginLog = new UserLoginLog();
            loginLog.setUserId(user.getId());
            loginLog.setLoginTime(now);
            loginLog.setCreateTime(now);
            userLoginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.warn("保存学生登录日志失败", e);
        }
    }

    private boolean isLocalRequest(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return "127.0.0.1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || "::1".equals(remoteAddr);
    }

    private User getSafetyUser(User user) {
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setUserName(user.getUserName());
        safetyUser.setUserAvatar(user.getUserAvatar());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setTeacherTitle(user.getTeacherTitle());
        safetyUser.setTeacherRegisterCode(user.getTeacherRegisterCode());
        safetyUser.setUserProfile(user.getUserProfile());
        safetyUser.setClassId(user.getClassId());
        safetyUser.setCreateTime(user.getCreateTime());
        return safetyUser;
    }

    private static class CaptchaEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        final String code;
        final long expireAt;

        CaptchaEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
