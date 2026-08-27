package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AdminAuditLogMapper;
import com.ruyi.teach.model.entity.AdminAuditLog;
import com.ruyi.teach.model.entity.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/audit-log")
public class AdminAuditController {


    @Resource
    private AdminAuditLogMapper adminAuditLogMapper;

    @GetMapping("/list")
    public BaseResponse<Page<AdminAuditLog>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        getAdminLoginUser(request);

        QueryWrapper<AdminAuditLog> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(module) && !"all".equals(module)) {
            wrapper.eq("module", module.trim());
        }
        if (StringUtils.isNotBlank(keyword)) {
            String text = keyword.trim();
            wrapper.and(w -> w.like("admin_account", text)
                    .or().like("admin_name", text)
                    .or().like("action", text)
                    .or().like("summary", text)
                    .or().like("target_id", text));
        }
        wrapper.orderByDesc("create_time");
        return ResultUtils.success(adminAuditLogMapper.selectPage(new Page<>(current, size), wrapper));
    }

    private User getAdminLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可访问");
        }
        return loginUser;
    }
}
