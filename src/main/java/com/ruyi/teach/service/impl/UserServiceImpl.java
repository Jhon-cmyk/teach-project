package com.ruyi.teach.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.cache.CaptchaStore;
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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Date;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 验证码答案在进程内的兜底存储前缀（仅 Redis 不可用时使用）。
     *
     * <p>这里<b>不再有</b> Session 版浏览器绑定：绑定已改为签名 Cookie（见
     * {@link CaptchaStore}）。原因是用实例私有的 Session 做绑定，会同时废掉
     * "验证码跨实例可用"这个目的——A 写入的绑定 B 读不到。
     */
    private static final String CAPTCHA_SESSION_PREFIX = "captcha:";

    /** 兜底 Session 存储的有效期，与 {@link CaptchaStore} 中的 TTL 保持一致。 */
    private static final long CAPTCHA_TTL_MS = 5 * 60 * 1000L;

    @Resource
    private CaptchaStore captchaStore;

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
    public CaptchaVO generateCaptcha(HttpServletRequest request, HttpServletResponse response) {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 20);
        String code = captcha.getCode();

        // 优先写入 Redis，使"A 实例取码、B 实例校验"也能通过。
        String captchaId = captchaStore.create(code);
        if (captchaId == null) {
            // Redis 不可用：退回进程内 Session 存储答案。单实例下行为与改造前完全一致；
            // 多实例下该验证码只在当前实例有效，属于可用性优先的降级。
            captchaId = IdUtil.simpleUUID();
            request.getSession().setAttribute(CAPTCHA_SESSION_PREFIX + captchaId,
                    new CaptchaEntry(code, System.currentTimeMillis() + CAPTCHA_TTL_MS));
        }

        // 浏览器绑定：签名 Cookie，任何实例都能独立验签（用 Session 会破坏跨实例能力）
        writeCaptchaBindingCookie(response, captchaStore.issueBinding(captchaId));

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

        // 先确认该验证码确实由当前浏览器申请，再消耗答案：
        // 缺少绑定校验时任何人都能重放别人的 captchaId，验证码会失去防机器人意义。
        if (!captchaStore.verifyBinding(captchaId, readCaptchaBindingToken(request))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码与当前浏览器不匹配，请刷新");
        }

        // 先取 Redis（跨实例可用），失败再取本次进程的 Session 兜底（降级路径）
        String expectedCode = captchaStore.consume(captchaId);
        if (expectedCode == null) {
            expectedCode = consumeSessionCaptcha(request.getSession(), CAPTCHA_SESSION_PREFIX + captchaId);
        }

        if (expectedCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已失效，请刷新");
        }
        if (!expectedCode.equalsIgnoreCase(captchaCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }

        return userLogin(loginRequest.getUserAccount(), loginRequest.getUserPassword(), request);
    }

    /** 写入浏览器绑定 Cookie：HttpOnly 防脚本读取，SameSite=Lax 防跨站带上。 */
    private void writeCaptchaBindingCookie(HttpServletResponse response, String bindingToken) {
        ResponseCookie cookie = ResponseCookie.from(CaptchaStore.BINDING_COOKIE, bindingToken)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /** 读取浏览器绑定令牌；不存在时返回 null。 */
    private String readCaptchaBindingToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CaptchaStore.BINDING_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** 读取进程内 Session 中兜底的验证码，处理过期并一次性移除。 */
    private String consumeSessionCaptcha(HttpSession session, String sessionKey) {
        Object attribute = session.getAttribute(sessionKey);
        if (!(attribute instanceof CaptchaEntry entry)) {
            return null;
        }
        session.removeAttribute(sessionKey);
        if (System.currentTimeMillis() > entry.expireAt) {
            return null;
        }
        return entry.code;
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
