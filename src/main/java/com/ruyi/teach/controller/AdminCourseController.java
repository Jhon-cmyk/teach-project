package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CourseCategoryMapper;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseCategory;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/course")
@Tag(name = "管理端平台课程管理")
public class AdminCourseController {

    @Resource
    private CourseService courseService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private UserService userService;

    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Operation(summary = "管理端课程列表")
    @GetMapping("/list")
    public BaseResponse<Page<AdminCourseVO>> listCourses(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String publishStatus,
            HttpServletRequest request) {

        getAdminLoginUser(request);

        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();

        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name.trim());
        }

        if (StringUtils.isBlank(sourceType)) {
            queryWrapper.eq("sourceType", "platform");
        } else if (!"all".equals(sourceType)) {
            queryWrapper.eq("sourceType", sourceType);
        }

        if (StringUtils.isNotBlank(publishStatus)) {
            queryWrapper.eq("publishStatus", publishStatus);
        }

        queryWrapper.orderByDesc("createTime");

        Page<Course> page = courseService.page(new Page<>(current, size), queryWrapper);
        List<Course> records = page.getRecords();

        Set<Long> creatorIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        for (Course course : records) {
            if (course.getCreatorId() != null) {
                creatorIds.add(course.getCreatorId());
            } else if (course.getTeacherId() != null) {
                creatorIds.add(course.getTeacherId());
            }
            if (course.getCategoryId() != null) {
                categoryIds.add(course.getCategoryId());
            }
        }

        final Map<Long, String> creatorNameMap;
        if (!creatorIds.isEmpty()) {
            List<User> userList = userService.listByIds(creatorIds);
            creatorNameMap = userList.stream()
                    .collect(Collectors.toMap(
                            User::getId,
                            user -> StringUtils.defaultIfBlank(user.getUserName(), user.getUserAccount())
                    ));
        } else {
            creatorNameMap = new HashMap<>();
        }

        final Map<Long, String> categoryNameMap;
        if (!categoryIds.isEmpty()) {
            List<CourseCategory> categoryList = courseCategoryMapper.selectBatchIds(categoryIds);
            categoryNameMap = categoryList.stream()
                    .collect(Collectors.toMap(
                            CourseCategory::getId,
                            CourseCategory::getName,
                            (a, b) -> a
                    ));
        } else {
            categoryNameMap = new HashMap<>();
        }

        List<AdminCourseVO> voList = records.stream()
                .map(course -> toCourseVO(course, creatorNameMap, categoryNameMap))
                .collect(Collectors.toList());

        Page<AdminCourseVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(voList);

        return ResultUtils.success(resultPage);
    }

    @Operation(summary = "管理端新增平台课程")
    @PostMapping("/add")
    public BaseResponse<Long> addCourse(@RequestBody AdminCourseEditRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || StringUtils.isBlank(requestBody.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程名称不能为空");
        }
        validateCategory(requestBody.getCategoryId());

        Course course = new Course();
        course.setName(requestBody.getName().trim());
        course.setDescription(requestBody.getDescription());
        course.setCoverImg(requestBody.getCoverImg());
        course.setVideoUrl(requestBody.getVideoUrl());
        course.setType(StringUtils.isNotBlank(requestBody.getType()) ? requestBody.getType() : "video");
        course.setSourceType("platform");
        course.setCreatorId(adminUser.getId());
        course.setCreatorRole("admin");
        course.setPublishStatus(StringUtils.isNotBlank(requestBody.getPublishStatus()) ? requestBody.getPublishStatus() : "published");
        course.setCategoryId(requestBody.getCategoryId());

        course.setTeacherId(adminUser.getId());
        course.setTeacherName(StringUtils.isNotBlank(adminUser.getUserName()) ? adminUser.getUserName() : "平台管理员");

        boolean result = courseService.save(course);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增课程失败");
        }

        adminAuditLogger.log(adminUser, "平台课程", "新增平台课程", "course", course.getId(),
                course.getName(), request);
        return ResultUtils.success(course.getId());
    }

    @Operation(summary = "管理端编辑平台课程")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateCourse(@RequestBody AdminCourseEditRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程 ID 不能为空");
        }

        Course oldCourse = courseService.getById(requestBody.getId());
        if (oldCourse == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }

        if (!"platform".equals(oldCourse.getSourceType())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "教师课程请在教师端管理");
        }
        validateCategory(requestBody.getCategoryId());

        Course updateCourse = new Course();
        updateCourse.setId(requestBody.getId());
        updateCourse.setName(StringUtils.isNotBlank(requestBody.getName()) ? requestBody.getName().trim() : oldCourse.getName());
        updateCourse.setDescription(requestBody.getDescription());
        updateCourse.setCoverImg(requestBody.getCoverImg());
        updateCourse.setVideoUrl(requestBody.getVideoUrl());
        updateCourse.setType(StringUtils.isNotBlank(requestBody.getType()) ? requestBody.getType() : oldCourse.getType());
        updateCourse.setSourceType("platform");
        updateCourse.setCreatorId(oldCourse.getCreatorId() != null ? oldCourse.getCreatorId() : adminUser.getId());
        updateCourse.setCreatorRole(StringUtils.isNotBlank(oldCourse.getCreatorRole()) ? oldCourse.getCreatorRole() : "admin");
        updateCourse.setPublishStatus(StringUtils.isNotBlank(requestBody.getPublishStatus()) ? requestBody.getPublishStatus() : oldCourse.getPublishStatus());
        updateCourse.setCategoryId(requestBody.getCategoryId());
        updateCourse.setTeacherId(oldCourse.getTeacherId() != null ? oldCourse.getTeacherId() : adminUser.getId());
        updateCourse.setTeacherName(StringUtils.isNotBlank(oldCourse.getTeacherName()) ? oldCourse.getTeacherName() : "平台管理员");

        boolean result = courseService.updateById(updateCourse);
        if (result) {
            adminAuditLogger.log(adminUser, "平台课程", "编辑平台课程", "course", requestBody.getId(),
                    updateCourse.getName(), request);
        }
        return ResultUtils.success(result);
    }

    @Operation(summary = "管理端删除平台课程")
    @PostMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> deleteCourse(@RequestBody IdRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程 ID 不能为空");
        }

        Course course = courseService.getById(requestBody.getId());
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }

        if (!"platform".equals(course.getSourceType())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "教师课程请在教师端管理");
        }

        courseChapterService.remove(new QueryWrapper<CourseChapter>().eq("course_id", requestBody.getId()));
        boolean result = courseService.removeById(requestBody.getId());
        if (result) {
            adminAuditLogger.log(adminUser, "平台课程", "删除平台课程", "course", requestBody.getId(),
                    course.getName(), request);
        }
        return ResultUtils.success(result);
    }

    private AdminCourseVO toCourseVO(Course course, Map<Long, String> creatorNameMap, Map<Long, String> categoryNameMap) {
        AdminCourseVO vo = new AdminCourseVO();
        vo.setId(course.getId());
        vo.setName(course.getName());
        vo.setDescription(course.getDescription());
        vo.setCoverImg(course.getCoverImg());
        vo.setVideoUrl(course.getVideoUrl());
        vo.setType(course.getType());
        vo.setTeacherId(course.getTeacherId());
        vo.setTeacherName(course.getTeacherName());
        vo.setSourceType(course.getSourceType());
        vo.setCreatorId(course.getCreatorId());
        vo.setCreatorRole(course.getCreatorRole());
        vo.setPublishStatus(course.getPublishStatus());
        vo.setCategoryId(course.getCategoryId());
        vo.setCategoryName(course.getCategoryId() == null ? null : categoryNameMap.get(course.getCategoryId()));
        vo.setCreateTime(course.getCreateTime());
        vo.setUpdateTime(course.getUpdateTime());

        Long creatorId = course.getCreatorId() != null ? course.getCreatorId() : course.getTeacherId();
        String creatorName = creatorId != null ? creatorNameMap.get(creatorId) : null;
        if (StringUtils.isBlank(creatorName)) {
            creatorName = StringUtils.defaultIfBlank(course.getTeacherName(), "未命名创建人");
        }
        vo.setCreatorName(creatorName);

        return vo;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择课程分类");
        }
        CourseCategory category = courseCategoryMapper.selectById(categoryId);
        if (category == null || category.getIsEnabled() == null || category.getIsEnabled() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择学生端已启用的课程分类");
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
    public static class AdminCourseEditRequest {
        private Long id;
        private String name;
        private String description;
        private String coverImg;
        private String videoUrl;
        private String type;
        private String publishStatus;
        private Long categoryId;
    }

    @Data
    public static class IdRequest {
        private Long id;
    }

    @Data
    public static class AdminCourseVO {
        private Long id;
        private String name;
        private String description;
        private String coverImg;
        private String videoUrl;
        private String type;
        private Long teacherId;
        private String teacherName;
        private String sourceType;
        private Long creatorId;
        private String creatorRole;
        private String creatorName;
        private String publishStatus;
        private Long categoryId;
        private String categoryName;
        private Date createTime;
        private Date updateTime;
    }
}
