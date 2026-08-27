package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.mapper.CourseCategoryMapper;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseCategory;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/category")
@Tag(name = "管理端课程分类图标管理")
public class AdminCategoryController {


    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Operation(summary = "分类图标列表")
    @GetMapping("/list")
    public BaseResponse<Page<CourseCategory>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {

        getAdminLoginUser(request);

        QueryWrapper<CourseCategory> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name.trim());
        }
        queryWrapper.orderByAsc("sort_order").orderByDesc("create_time");

        Page<CourseCategory> page = new Page<>(current, size);
        Page<CourseCategory> result = courseCategoryMapper.selectPage(page, queryWrapper);
        return ResultUtils.success(result);
    }

    @Operation(summary = "新增分类图标")
    @PostMapping("/add")
    public BaseResponse<Long> add(@RequestBody CourseCategory category, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (category == null || StringUtils.isBlank(category.getName()) || StringUtils.isBlank(category.getIconUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称和图标不能为空");
        }

        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getIsEnabled() == null) {
            category.setIsEnabled(1);
        }
        if (category.getIsEnabled() == 1) {
            ensureEnabledSlotAvailable(null);
        }

        courseCategoryMapper.insert(category);
        adminAuditLogger.log(adminUser, "运营素材", "新增课程分类", "course_category", category.getId(),
                category.getName(), request);
        return ResultUtils.success(category.getId());
    }

    @Operation(summary = "更新分类图标")
    @PostMapping("/update")
    public BaseResponse<Boolean> update(@RequestBody CourseCategory category, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (category == null || category.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类 ID 不能为空");
        }

        CourseCategory oldCategory = courseCategoryMapper.selectById(category.getId());
        if (oldCategory == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分类不存在");
        }
        if (category.getIsEnabled() != null
                && category.getIsEnabled() == 1
                && (oldCategory.getIsEnabled() == null || oldCategory.getIsEnabled() != 1)) {
            ensureEnabledSlotAvailable(category.getId());
        }

        boolean result = courseCategoryMapper.updateById(category) > 0;
        if (result) {
            adminAuditLogger.log(adminUser, "运营素材", "更新课程分类", "course_category", category.getId(),
                    StringUtils.defaultIfBlank(category.getName(), oldCategory.getName()), request);
        }
        return ResultUtils.success(result);
    }

    @Operation(summary = "删除分类图标")
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody IdRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类 ID 不能为空");
        }

        CourseCategory oldCategory = courseCategoryMapper.selectById(requestBody.getId());
        Long courseCount = courseMapper.selectCount(new QueryWrapper<Course>().eq("categoryId", requestBody.getId()));
        if (courseCount != null && courseCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已有课程关联该分类，请先调整课程分类后再删除");
        }

        boolean result = courseCategoryMapper.deleteById(requestBody.getId()) > 0;
        if (result) {
            adminAuditLogger.log(adminUser, "运营素材", "删除课程分类", "course_category", requestBody.getId(),
                    oldCategory == null ? "" : oldCategory.getName(), request);
        }
        return ResultUtils.success(result);
    }

    private void ensureEnabledSlotAvailable(Long currentId) {
        QueryWrapper<CourseCategory> wrapper = new QueryWrapper<CourseCategory>().eq("is_enabled", 1);
        if (currentId != null) {
            wrapper.ne("id", currentId);
        }
        Long enabledCount = courseCategoryMapper.selectCount(wrapper);
        if (enabledCount != null && enabledCount >= 18) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "学生端分类图标最多展示 18 个，请先停用其他分类");
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
    public static class IdRequest {
        private Long id;
    }
}
