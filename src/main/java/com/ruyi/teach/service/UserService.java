package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.CaptchaLoginRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.CaptchaVO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {
    /**
     * 用户登录
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request      请求对象
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户注册
     *
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 校验密码
     * @param userName      用户昵称
     * @param userRole      用户角色 (student / teacher)
     * @return 新用户 ID
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String userName, String userRole, String teacherRegisterCode);

    /**
     * 修改密码 (包含加密验证逻辑)
     *
     * @param id           用户ID
     * @param oldPassword  旧密码(明文)
     * @param newPassword  新密码(明文)
     * @return 是否修改成功
     */
    boolean updateUserPassword(Long id, String oldPassword, String newPassword);

    /**
     * 生成图形验证码（存入 session，5 分钟有效）
     */
    CaptchaVO generateCaptcha(HttpServletRequest request);

    /**
     * 账号密码 + 图形验证码登录
     */
    User userLoginByCaptcha(CaptchaLoginRequest loginRequest, HttpServletRequest request);
}
