package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.CourseCreateRequest;
import com.ruyi.teach.model.dto.CourseUpdateRequest;
import com.ruyi.teach.model.dto.IdRequest;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.enums.UserRole;
import com.ruyi.teach.model.vo.CourseVO;
import com.ruyi.teach.model.vo.ResourcePreviewVO;
import com.ruyi.teach.model.vo.ResourceSearchPageVO;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.RoleAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/course")
@Tag(name = "课程管理")
public class CourseController {

    @Resource
    private CourseService courseService;

    @Resource
    private RoleAuthorizationService roleAuthorizationService;

    @Resource
    private com.ruyi.teach.mapper.CourseMapper courseMapper;

    @Resource
    private com.ruyi.teach.mapper.CourseClassRelationMapper courseClassRelationMapper;

    /**
     * 教师端：获取我发布的课程
     */
    @Operation(summary = "获取我发布的课程")
    @GetMapping("/list/page")
    public BaseResponse<Page<Course>> listMyCourses(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {

        User loginUser = requireTeacher(request);

        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teacherId", loginUser.getId());

        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name.trim());
        }

        queryWrapper.orderByDesc("createTime");
        Page<Course> page = courseService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(page);
    }

    /**
     * 教师端：新增课程
     */
    @Operation(summary = "教师新增课程")
    @PostMapping("/add")
    public BaseResponse<Long> addCourse(@Valid @RequestBody CourseCreateRequest requestBody,
                                        HttpServletRequest request) {
        User loginUser = requireTeacher(request);
        Course course = toCourse(requestBody);

        course.setTeacherId(loginUser.getId());
        course.setTeacherName(StringUtils.isNotBlank(loginUser.getUserName()) ? loginUser.getUserName() : "金牌讲师");

        if (StringUtils.isBlank(course.getType())) {
            course.setType("video");
        }
        if (StringUtils.isBlank(course.getSourceType())) {
            course.setSourceType("teacher");
        }
        if (course.getCreatorId() == null) {
            course.setCreatorId(loginUser.getId());
        }
        if (StringUtils.isBlank(course.getCreatorRole())) {
            course.setCreatorRole("teacher");
        }
        if (StringUtils.isBlank(course.getPublishStatus())) {
            course.setPublishStatus("published");
        }
        if (course.getFaceDetectionRequired() == null) {
            course.setFaceDetectionRequired(false);
        }

        boolean result = courseService.saveCourseWithClasses(course);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(course.getId());
    }

    /**
     * 教师端：更新课程
     */
    @Operation(summary = "教师更新课程")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateCourse(@Valid @RequestBody CourseUpdateRequest requestBody,
                                              HttpServletRequest request) {
        User loginUser = requireTeacher(request);
        Course course = toCourse(requestBody);
        Course oldCourse = requireOwnedCourse(course.getId(), loginUser);

        course.setTeacherId(oldCourse.getTeacherId());
        course.setTeacherName(oldCourse.getTeacherName());
        if (StringUtils.isBlank(course.getType())) {
            course.setType(oldCourse.getType());
        }
        if (StringUtils.isBlank(course.getSourceType())) {
            course.setSourceType(StringUtils.isNotBlank(oldCourse.getSourceType()) ? oldCourse.getSourceType() : "teacher");
        }
        course.setCreatorId(oldCourse.getCreatorId() != null ? oldCourse.getCreatorId() : oldCourse.getTeacherId());
        course.setCreatorRole(StringUtils.isNotBlank(oldCourse.getCreatorRole()) ? oldCourse.getCreatorRole() : "teacher");
        if (StringUtils.isBlank(course.getPublishStatus())) {
            course.setPublishStatus(StringUtils.isNotBlank(oldCourse.getPublishStatus()) ? oldCourse.getPublishStatus() : "published");
        }
        if (course.getFaceDetectionRequired() == null) {
            course.setFaceDetectionRequired(Boolean.TRUE.equals(oldCourse.getFaceDetectionRequired()));
        }

        boolean result = courseService.updateCourseWithClasses(course);
        return ResultUtils.success(result);
    }

    /**
     * 教师端：删除课程
     */
    @Operation(summary = "教师删除课程")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteCourse(@Valid @RequestBody IdRequest requestBody,
                                              HttpServletRequest request) {
        User loginUser = requireTeacher(request);
        requireOwnedCourse(requestBody.getId(), loginUser);
        boolean result = courseService.removeById(requestBody.getId());
        return ResultUtils.success(result);
    }

    /**
     * 公共：课程列表
     */
    @Operation(summary = "获取所有课程(公共)")
    @GetMapping("/list/all")
    public BaseResponse<Page<Course>> listAllCourses(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId) {

        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", "video");
        queryWrapper.eq("publishStatus", "published");
        queryWrapper.eq("sourceType", "platform");

        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name.trim());
        }
        if (categoryId != null) {
            queryWrapper.eq("categoryId", categoryId);
        }

        queryWrapper.orderByDesc("createTime");
        Page<Course> page = courseService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(page);
    }

    /**
     * 学生：获取自己班级排课
     */
    @Operation(summary = "获取学生所在班级的专属课程")
    @GetMapping("/list/my-class")
    public BaseResponse<List<CourseVO>> listMyClassCourses(HttpServletRequest request) {
        User loginUser = getLoginUser(request);

        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可查看班级排课");
        }

        List<CourseVO> courses = courseMapper.selectStudentCourses(loginUser.getId());
        return ResultUtils.success(courses);
    }

    /**
     * 登录态获取
     */
    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return loginUser;
    }

    private User requireTeacher(HttpServletRequest request) {
        return roleAuthorizationService.requireAnyRole(getLoginUser(request), UserRole.TEACHER);
    }

    private Course requireOwnedCourse(Long courseId, User loginUser) {
        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }
        roleAuthorizationService.requireOwner(loginUser, course.getTeacherId(), "课程");
        return course;
    }

    private Course toCourse(CourseCreateRequest requestBody) {
        Course course = new Course();
        course.setName(requestBody.getName());
        course.setDescription(requestBody.getDescription());
        course.setCoverImg(requestBody.getCoverImg());
        course.setType(requestBody.getType());
        course.setFaceDetectionRequired(requestBody.getFaceDetectionRequired());
        course.setClassIds(requestBody.getClassIds());
        return course;
    }

    private Course toCourse(CourseUpdateRequest requestBody) {
        Course course = new Course();
        course.setId(requestBody.getId());
        course.setName(requestBody.getName());
        course.setDescription(requestBody.getDescription());
        course.setCoverImg(requestBody.getCoverImg());
        course.setType(requestBody.getType());
        course.setFaceDetectionRequired(requestBody.getFaceDetectionRequired());
        course.setClassIds(requestBody.getClassIds());
        return course;
    }

    @Operation(summary = "资源检索页：分页检索内部课程资源")
    @GetMapping("/search/resources")
    public BaseResponse<ResourceSearchPageVO> searchResources(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "6") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "relevance") String sortMode,
            HttpServletRequest request) {

        ResourceSearchPageVO result = courseService.searchPublicResourcePage(keyword, type, current, pageSize, sortMode, getLoginUser(request));
        return ResultUtils.success(result);
    }

    @Operation(summary = "学生端资源预览详情")
    @GetMapping("/search/resource-preview")
    public BaseResponse<ResourcePreviewVO> getResourcePreview(
            @RequestParam Long id,
            @RequestParam String type,
            HttpServletRequest request) {
        return ResultUtils.success(courseService.getResourcePreview(id, type, getLoginUser(request)));
    }

    /**
     * 获取指定课程已分配的班级ID列表（用于前端编辑页回显）
     */
    @Operation(summary = "获取课程绑定的班级ID列表")
    @GetMapping("/classIds")
    public BaseResponse<List<Long>> getCourseClassIds(@RequestParam Long courseId,
                                                       HttpServletRequest request) {
        if (courseId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = requireTeacher(request);
        requireOwnedCourse(courseId, loginUser);

        // 去关联表中查询这个 courseId 对应的所有 classId
        QueryWrapper<com.ruyi.teach.model.entity.CourseClassRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId);
        List<com.ruyi.teach.model.entity.CourseClassRelation> relations = courseClassRelationMapper.selectList(queryWrapper);

        // 提取出 classId 的纯数组
        List<Long> classIds = relations.stream()
                .map(com.ruyi.teach.model.entity.CourseClassRelation::getClassId)
                .collect(Collectors.toList());

        return ResultUtils.success(classIds);
    }
}
