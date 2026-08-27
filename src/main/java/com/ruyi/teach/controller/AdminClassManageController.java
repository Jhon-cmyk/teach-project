package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.entity.SysClass;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/admin/class")
public class AdminClassManageController {


    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private UserService userService;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @GetMapping("/list")
    public BaseResponse<Page<SysClass>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String major,
            HttpServletRequest request) {
        getAdminLoginUser(request);

        QueryWrapper<SysClass> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            String text = keyword.trim();
            wrapper.and(w -> w.like("name", text).or().like("major", text).or().like("college", text));
        }
        if (StringUtils.isNotBlank(major) && !"all".equals(major)) {
            wrapper.eq("major", major.trim());
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");

        Page<SysClass> page = sysClassMapper.selectPage(new Page<>(current, size), wrapper);
        page.getRecords().forEach(item -> item.setStudentCount(Math.toIntExact(countStudents(item.getId()))));
        return ResultUtils.success(page);
    }

    @GetMapping("/majors")
    public BaseResponse<List<String>> majors(HttpServletRequest request) {
        getAdminLoginUser(request);
        List<String> majors = sysClassMapper.selectObjs(new QueryWrapper<SysClass>()
                        .select("DISTINCT major")
                        .isNotNull("major")
                        .ne("major", "")
                        .orderByAsc("major"))
                .stream()
                .map(String::valueOf)
                .toList();
        return ResultUtils.success(majors);
    }

    @PostMapping("/save")
    public BaseResponse<Boolean> save(@RequestBody ClassSaveRequest body, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        validate(body);

        SysClass entity = new SysClass();
        entity.setName(body.getName().trim());
        entity.setMajor(StringUtils.trimToEmpty(body.getMajor()));
        entity.setCollege(StringUtils.trimToEmpty(body.getCollege()));

        boolean result;
        String action;
        if (body.getId() == null) {
            entity.setCreateTime(new Date());
            result = sysClassMapper.insert(entity) > 0;
            action = "新增班级";
        } else {
            SysClass old = sysClassMapper.selectById(body.getId());
            if (old == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "班级不存在");
            }
            entity.setId(body.getId());
            result = sysClassMapper.updateById(entity) > 0;
            action = "修改班级";
        }

        adminAuditLogger.log(admin, "班级专业管理", action, "sys_class", entity.getId(),
                entity.getName() + " / " + entity.getMajor(), request);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody IdRequest body, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        if (body == null || body.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "班级 ID 不能为空");
        }
        SysClass target = sysClassMapper.selectById(body.getId());
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "班级不存在");
        }
        long studentCount = countStudents(body.getId());
        if (studentCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该班级下还有学生，不能删除");
        }
        boolean result = sysClassMapper.deleteById(body.getId()) > 0;
        adminAuditLogger.log(admin, "班级专业管理", "删除班级", "sys_class", body.getId(), target.getName(), request);
        return ResultUtils.success(result);
    }

    private long countStudents(Long classId) {
        return userService.count(new QueryWrapper<User>()
                .eq("class_id", classId)
                .eq("userRole", "student"));
    }

    private void validate(ClassSaveRequest body) {
        if (body == null || StringUtils.isBlank(body.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "班级名称不能为空");
        }
        if (StringUtils.length(body.getName()) > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "班级名称过长");
        }
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

    @Data
    public static class ClassSaveRequest {
        private Long id;
        private String name;
        private String major;
        private String college;
    }

    @Data
    public static class IdRequest {
        private Long id;
    }
}
