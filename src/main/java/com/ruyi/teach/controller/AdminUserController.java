package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.mapper.TeacherRegistrationCodeMapper;
import com.ruyi.teach.model.entity.SysClass;
import com.ruyi.teach.model.entity.TeacherRegistrationCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/user")
@Tag(name = "管理端用户管理")
public class AdminUserController {

    @Resource
    private UserService userService;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private TeacherRegistrationCodeMapper teacherRegistrationCodeMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Operation(summary = "管理端用户列表")
    @GetMapping("/list")
    public BaseResponse<Page<AdminUserVO>> listUsers(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) Long classId,
            HttpServletRequest request) {

        getAdminLoginUser(request);

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        if (StringUtils.isNotBlank(role) && !"all".equals(role)) {
            queryWrapper.eq("userRole", role);
        }

        if (StringUtils.isNotBlank(keyword)) {
            String text = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like("userAccount", text)
                    .or()
                    .like("userName", text)
                    .or()
                    .like("teacher_title", text)
                    .or()
                    .like("teacher_register_code", text));
        }

        if (StringUtils.isNotBlank(major) && !"all".equals(major)) {
            List<Long> classIds = sysClassMapper.selectList(new QueryWrapper<SysClass>()
                            .eq("major", major.trim()))
                    .stream()
                    .map(SysClass::getId)
                    .collect(Collectors.toList());

            if (classIds.isEmpty()) {
                queryWrapper.eq("class_id", -1L);
            } else {
                queryWrapper.in("class_id", classIds);
            }
        }

        if (classId != null) {
            queryWrapper.eq("class_id", classId);
        }

        queryWrapper.orderByDesc("createTime");

        Page<User> page = userService.page(new Page<>(current, size), queryWrapper);
        Map<Long, String> classNameMap = buildClassNameMap(page.getRecords());
        List<AdminUserVO> records = page.getRecords().stream()
                .map(user -> toUserVO(user, classNameMap))
                .collect(Collectors.toList());

        Page<AdminUserVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(records);

        return ResultUtils.success(resultPage);
    }

    @Operation(summary = "管理端用户角色不可修改")
    @PostMapping("/update-role")
    public BaseResponse<Boolean> updateUserRole(@RequestBody Object requestBody, HttpServletRequest request) {
        getAdminLoginUser(request);
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "管理员不能修改账号身份");
    }

    @Operation(summary = "管理端设置学生班级")
    @PostMapping("/update-class")
    public BaseResponse<Boolean> updateUserClass(@RequestBody UpdateUserClassRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        User targetUser = userService.getById(requestBody.getId());
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        if (!"student".equals(targetUser.getUserRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅学生可设置班级");
        }

        if (requestBody.getClassId() != null && sysClassMapper.selectById(requestBody.getClassId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "班级不存在");
        }

        User updateUser = new User();
        updateUser.setId(requestBody.getId());
        updateUser.setClassId(requestBody.getClassId());

        boolean result = userService.updateById(updateUser);
        adminAuditLogger.log(adminUser, "用户管理", "设置学生班级", "user", requestBody.getId(),
                "classId=" + requestBody.getClassId(), request);
        return ResultUtils.success(result);
    }

    @Operation(summary = "管理端设置教师职称")
    @PostMapping("/update-teacher-title")
    public BaseResponse<Boolean> updateTeacherTitle(@RequestBody UpdateTeacherTitleRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);
        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师 ID 不能为空");
        }
        if (StringUtils.isBlank(requestBody.getTeacherTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师职称不能为空");
        }

        User targetUser = userService.getById(requestBody.getId());
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "教师不存在");
        }
        if (!"teacher".equals(targetUser.getUserRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅教师可设置职称");
        }

        User updateUser = new User();
        updateUser.setId(requestBody.getId());
        updateUser.setTeacherTitle(StringUtils.trim(requestBody.getTeacherTitle()));
        boolean result = userService.updateById(updateUser);

        adminAuditLogger.log(adminUser, "用户管理", "设置教师职称", "user", requestBody.getId(),
                targetUser.getUserAccount() + " / " + updateUser.getTeacherTitle(), request);
        return ResultUtils.success(result);
    }

    @Operation(summary = "教师注册号列表")
    @GetMapping("/teacher-code/list")
    public BaseResponse<Page<TeacherRegistrationCode>> listTeacherCodes(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        getAdminLoginUser(request);

        QueryWrapper<TeacherRegistrationCode> wrapper = new QueryWrapper<>();
        wrapper.eq("is_delete", 0);
        if (StringUtils.isNotBlank(status) && !"all".equals(status)) {
            wrapper.eq("status", status.trim());
        }
        if (StringUtils.isNotBlank(keyword)) {
            String text = keyword.trim();
            wrapper.and(w -> w.like("register_code", text)
                    .or().like("teacher_name", text)
                    .or().like("teacher_title", text));
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        return ResultUtils.success(teacherRegistrationCodeMapper.selectPage(new Page<>(current, size), wrapper));
    }

    @Operation(summary = "创建教师注册号")
    @PostMapping("/teacher-code/create")
    public BaseResponse<Long> createTeacherCode(@RequestBody TeacherCodeCreateRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);
        if (requestBody == null || StringUtils.isBlank(requestBody.getTeacherTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师职称不能为空");
        }

        String registerCode = StringUtils.trimToEmpty(requestBody.getRegisterCode()).toUpperCase();
        if (StringUtils.isBlank(registerCode)) {
            registerCode = generateTeacherRegisterCode();
        }
        if (registerCode.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册号长度不能少于 4 位");
        }
        Long count = teacherRegistrationCodeMapper.selectCount(new QueryWrapper<TeacherRegistrationCode>()
                .eq("register_code", registerCode));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该注册号已存在");
        }

        TeacherRegistrationCode code = new TeacherRegistrationCode();
        code.setRegisterCode(registerCode);
        code.setTeacherName(StringUtils.trimToEmpty(requestBody.getTeacherName()));
        code.setTeacherTitle(StringUtils.trim(requestBody.getTeacherTitle()));
        code.setStatus("unused");
        code.setCreateTime(new Date());
        code.setUpdateTime(new Date());
        code.setIsDelete(0);
        teacherRegistrationCodeMapper.insert(code);

        adminAuditLogger.log(adminUser, "用户管理", "创建教师注册号", "teacher_registration_code", code.getId(),
                registerCode + " / " + code.getTeacherTitle(), request);
        return ResultUtils.success(code.getId());
    }

    @Operation(summary = "删除未使用教师注册号")
    @PostMapping("/teacher-code/delete")
    public BaseResponse<Boolean> deleteTeacherCode(@RequestBody IdRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);
        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册号 ID 不能为空");
        }
        TeacherRegistrationCode code = teacherRegistrationCodeMapper.selectById(requestBody.getId());
        if (code == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "注册号不存在");
        }
        if (!"unused".equals(code.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已使用的注册号不能删除");
        }
        boolean result = teacherRegistrationCodeMapper.deleteById(requestBody.getId()) > 0;
        adminAuditLogger.log(adminUser, "用户管理", "删除教师注册号", "teacher_registration_code", requestBody.getId(),
                code.getRegisterCode(), request);
        return ResultUtils.success(result);
    }

    @Operation(summary = "管理端删除用户")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody IdRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        if (adminUser.getId().equals(requestBody.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "不能删除当前登录管理员");
        }

        User targetUser = userService.getById(requestBody.getId());
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        boolean result = userService.removeById(requestBody.getId());
        adminAuditLogger.log(adminUser, "用户管理", "删除用户", "user", requestBody.getId(),
                StringUtils.defaultIfBlank(targetUser.getUserName(), targetUser.getUserAccount()), request);
        return ResultUtils.success(result);
    }

    private String generateTeacherRegisterCode() {
        return "T" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + RandomStringUtils.randomAlphanumeric(6).toUpperCase();
    }

    private Map<Long, String> buildClassNameMap(List<User> users) {
        Set<Long> classIds = users.stream()
                .map(User::getClassId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (classIds.isEmpty()) {
            return Map.of();
        }

        return sysClassMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(
                        SysClass::getId,
                        SysClass::getName,
                        (first, second) -> first
                ));
    }

    private AdminUserVO toUserVO(User user, Map<Long, String> classNameMap) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserRole(user.getUserRole());
        vo.setTeacherTitle(user.getTeacherTitle());
        vo.setTeacherRegisterCode(user.getTeacherRegisterCode());
        vo.setClassId(user.getClassId());
        vo.setCreateTime(user.getCreateTime());

        if (user.getClassId() == null) {
            vo.setClassDisplay("未分配");
        } else {
            String className = classNameMap.get(user.getClassId());
            vo.setClassName(className);
            vo.setClassDisplay(StringUtils.isNotBlank(className) ? className : "班级 ID：" + user.getClassId());
        }

        return vo;
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
    public static class UpdateUserClassRequest {
        private Long id;
        private Long classId;
    }

    @Data
    public static class UpdateTeacherTitleRequest {
        private Long id;
        private String teacherTitle;
    }

    @Data
    public static class TeacherCodeCreateRequest {
        private String registerCode;
        private String teacherName;
        private String teacherTitle;
    }

    @Data
    public static class IdRequest {
        private Long id;
    }

    @Data
    public static class AdminUserVO {
        private Long id;
        private String userAccount;
        private String userName;
        private String userRole;
        private String teacherTitle;
        private String teacherRegisterCode;
        private Long classId;
        private String className;
        private String classDisplay;
        private Date createTime;
    }
}
