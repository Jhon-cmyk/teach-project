package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/course/chapter")
@Tag(name = "管理端平台课程分集管理")
public class AdminCourseChapterController {

    @Resource
    private CourseService courseService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private AdminAuditLogger adminAuditLogger;

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

    private Course getPlatformCourseOrThrow(Long courseId) {
        if (courseId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }

        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }

        if (!"platform".equals(course.getSourceType())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅可管理平台课程分集");
        }

        return course;
    }

    @Operation(summary = "管理端获取平台课程分集列表")
    @GetMapping("/list")
    public BaseResponse<List<CourseChapter>> listChapters(
            @RequestParam Long courseId,
            HttpServletRequest request
    ) {
        getAdminLoginUser(request);
        getPlatformCourseOrThrow(courseId);

        QueryWrapper<CourseChapter> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId)
                .eq("is_delete", 0)
                .orderByAsc("sort_order")
                .orderByAsc("id");

        return ResultUtils.success(courseChapterService.list(wrapper));
    }

    @Operation(summary = "管理端整门课程覆盖保存分集")
    @PostMapping("/replace")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> replaceChapters(
            @RequestBody ReplaceChapterRequest requestBody,
            HttpServletRequest request
    ) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || requestBody.getCourseId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }

        Course course = getPlatformCourseOrThrow(requestBody.getCourseId());

        QueryWrapper<CourseChapter> removeWrapper = new QueryWrapper<>();
        removeWrapper.eq("course_id", requestBody.getCourseId());
        courseChapterService.remove(removeWrapper);

        List<ChapterItem> rawList = requestBody.getChapterList();
        List<CourseChapter> saveList = new ArrayList<>();

        if (rawList != null && !rawList.isEmpty()) {
            int sortSeed = 1;
            for (ChapterItem item : rawList) {
                if (item == null) {
                    continue;
                }

                String title = StringUtils.trimToEmpty(item.getTitle());
                String videoUrl = StringUtils.trimToEmpty(item.getVideoUrl());

                if (StringUtils.isBlank(title) && StringUtils.isBlank(videoUrl)) {
                    continue;
                }

                if (StringUtils.isBlank(title)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "分集标题不能为空");
                }

                if (StringUtils.isBlank(videoUrl)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "分集视频链接不能为空");
                }

                CourseChapter chapter = new CourseChapter();
                chapter.setCourseId(requestBody.getCourseId());
                chapter.setTitle(title);
                chapter.setVideoUrl(videoUrl);
                chapter.setSortOrder(item.getSortOrder() != null && item.getSortOrder() > 0
                        ? item.getSortOrder()
                        : sortSeed);

                saveList.add(chapter);
                sortSeed++;
            }
        }

        if (!saveList.isEmpty()) {
            courseChapterService.saveBatch(saveList);
        }

        // 兼容旧逻辑：把第一集视频同步到 course.videoUrl，避免旧页面/旧预览拿不到视频
        Course updateCourse = new Course();
        updateCourse.setId(course.getId());
        if (!saveList.isEmpty()) {
            updateCourse.setVideoUrl(saveList.get(0).getVideoUrl());
        } else {
            updateCourse.setVideoUrl("");
        }
        courseService.updateById(updateCourse);
        adminAuditLogger.log(adminUser, "平台课程", "保存课程分集", "course", requestBody.getCourseId(),
                course.getName() + "，分集数=" + saveList.size(), request);

        return ResultUtils.success(true);
    }

    @Data
    public static class ReplaceChapterRequest {
        private Long courseId;
        private List<ChapterItem> chapterList;
    }

    @Data
    public static class ChapterItem {
        private Long id;
        private String title;
        private String videoUrl;
        private Integer sortOrder;
    }
}
