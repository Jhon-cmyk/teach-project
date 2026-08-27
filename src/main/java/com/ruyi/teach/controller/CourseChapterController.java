package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.CourseChapterCreateRequest;
import com.ruyi.teach.model.dto.CourseChapterUpdateRequest;
import com.ruyi.teach.model.dto.IdRequest;
import com.ruyi.teach.model.dto.video.VideoKnowledgeSegmentSaveRequest;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.enums.UserRole;
import com.ruyi.teach.model.entity.VideoKnowledgeSegment;
import com.ruyi.teach.model.vo.VideoTimelineAnalysisTaskVO;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.RoleAuthorizationService;
import com.ruyi.teach.service.VideoKnowledgeSegmentService;
import com.ruyi.teach.service.VideoTimelineAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chapter")
@Tag(name = "课程章节(选集)管理")
@Slf4j
public class CourseChapterController {

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private CourseService courseService;

    @Resource
    private RoleAuthorizationService roleAuthorizationService;

    @Resource
    private VideoKnowledgeSegmentService videoKnowledgeSegmentService;

    @Resource
    private VideoTimelineAnalysisService videoTimelineAnalysisService;

    // 辅助方法：获取当前登录用户
    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
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
        roleAuthorizationService.requireOwner(loginUser, course.getTeacherId(), "课程章节");
        return course;
    }

    private CourseChapter toCourseChapter(CourseChapterCreateRequest requestBody) {
        CourseChapter chapter = new CourseChapter();
        chapter.setCourseId(requestBody.getCourseId());
        chapter.setTitle(requestBody.getTitle());
        chapter.setVideoUrl(requestBody.getVideoUrl());
        chapter.setSortOrder(requestBody.getSortOrder());
        chapter.setAnimHtml(requestBody.getAnimHtml());
        return chapter;
    }

    private CourseChapter toCourseChapter(CourseChapterUpdateRequest requestBody) {
        CourseChapter chapter = new CourseChapter();
        chapter.setId(requestBody.getId());
        chapter.setCourseId(requestBody.getCourseId());
        chapter.setTitle(requestBody.getTitle());
        chapter.setVideoUrl(requestBody.getVideoUrl());
        chapter.setSortOrder(requestBody.getSortOrder());
        chapter.setAnimHtml(requestBody.getAnimHtml());
        return chapter;
    }

    @Operation(summary = "新增选集(教师端)")
    @PostMapping("/add")
    public BaseResponse<Long> addChapter(@Valid @RequestBody CourseChapterCreateRequest requestBody,
                                         HttpServletRequest request) {
        User loginUser = requireTeacher(request);
        CourseChapter courseChapter = toCourseChapter(requestBody);
        requireOwnedCourse(courseChapter.getCourseId(), loginUser);

        boolean result = courseChapterService.save(courseChapter);
        if (!result) throw new BusinessException(ErrorCode.OPERATION_ERROR);
        triggerAutoTimelineAnalysisIfVideoReady(courseChapter, loginUser);
        return ResultUtils.success(courseChapter.getId());
    }

    @Operation(summary = "更新选集信息(教师端)")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateChapter(@Valid @RequestBody CourseChapterUpdateRequest requestBody,
                                               HttpServletRequest request) {
        User loginUser = requireTeacher(request);
        CourseChapter courseChapter = toCourseChapter(requestBody);

        CourseChapter oldChapter = courseChapterService.getById(courseChapter.getId());
        if (oldChapter == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "章节不存在");
        }
        requireOwnedCourse(oldChapter.getCourseId(), loginUser);
        if (courseChapter.getCourseId() != null
                && !courseChapter.getCourseId().equals(oldChapter.getCourseId())) {
            requireOwnedCourse(courseChapter.getCourseId(), loginUser);
        }
        boolean result = courseChapterService.updateById(courseChapter);
        if (result
                && StringUtils.isNotBlank(courseChapter.getVideoUrl())
                && (oldChapter == null || !StringUtils.equals(oldChapter.getVideoUrl(), courseChapter.getVideoUrl()))) {
            triggerAutoTimelineAnalysisIfVideoReady(courseChapter, loginUser);
        }
        return ResultUtils.success(result);
    }

    @Operation(summary = "删除选集(教师端)")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChapter(@Valid @RequestBody IdRequest requestBody,
                                               HttpServletRequest request) {
        User loginUser = requireTeacher(request);
        CourseChapter existingChapter = courseChapterService.getById(requestBody.getId());
        if (existingChapter == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "章节不存在");
        }
        requireOwnedCourse(existingChapter.getCourseId(), loginUser);

        boolean result = courseChapterService.removeById(requestBody.getId());
        return ResultUtils.success(result);
    }

    @Operation(summary = "获取某门课程的完整播放列表(公共接口)")
    @GetMapping("/list")
    public BaseResponse<List<CourseChapter>> listChaptersByCourseId(@RequestParam Long courseId) {
        if (courseId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }
        
        // 核心逻辑：根据课程ID查询，并严格按照 sort_order 升序排列
        QueryWrapper<CourseChapter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                    .eq("is_delete", 0)
                    .orderByAsc("sort_order");
                    
        List<CourseChapter> chapterList = courseChapterService.list(queryWrapper);
        return ResultUtils.success(chapterList);
    }

    @Operation(summary = "获取章节视频知识点时间轴")
    @GetMapping("/{chapterId}/segments")
    public BaseResponse<List<VideoKnowledgeSegment>> listSegments(@PathVariable Long chapterId) {
        return ResultUtils.success(videoKnowledgeSegmentService.listByChapterId(chapterId));
    }

    @Operation(summary = "保存章节视频知识点时间轴")
    @PostMapping("/{chapterId}/segments/save")
    public BaseResponse<Boolean> saveSegments(@PathVariable Long chapterId,
                                              @RequestBody VideoKnowledgeSegmentSaveRequest request,
                                              HttpServletRequest httpRequest) {
        User loginUser = getLoginUser(httpRequest);
        return ResultUtils.success(videoKnowledgeSegmentService.saveChapterSegments(chapterId, request, loginUser));
    }

    @Operation(summary = "AI 生成章节视频知识点时间轴草稿")
    @PostMapping("/{chapterId}/segments/ai-analysis/start")
    public BaseResponse<Long> startSegmentAiAnalysis(@PathVariable Long chapterId,
                                                     HttpServletRequest httpRequest) {
        User loginUser = getLoginUser(httpRequest);
        return ResultUtils.success(videoTimelineAnalysisService.startAnalysis(chapterId, loginUser));
    }

    @Operation(summary = "查询章节最近一次 AI 时间轴任务")
    @GetMapping("/{chapterId}/segments/ai-analysis/latest")
    public BaseResponse<VideoTimelineAnalysisTaskVO> getLatestSegmentAiAnalysisTask(@PathVariable Long chapterId,
                                                                                    HttpServletRequest httpRequest) {
        User loginUser = getLoginUser(httpRequest);
        return ResultUtils.success(videoTimelineAnalysisService.getLatestTask(chapterId, loginUser));
    }

    @Operation(summary = "查询 AI 生成知识点时间轴任务")
    @GetMapping("/{chapterId}/segments/ai-analysis/{taskId}")
    public BaseResponse<VideoTimelineAnalysisTaskVO> getSegmentAiAnalysisTask(@PathVariable Long chapterId,
                                                                              @PathVariable Long taskId,
                                                                              HttpServletRequest httpRequest) {
        User loginUser = getLoginUser(httpRequest);
        return ResultUtils.success(videoTimelineAnalysisService.getTask(chapterId, taskId, loginUser));
    }

    private void triggerAutoTimelineAnalysisIfVideoReady(CourseChapter chapter, User loginUser) {
        if (chapter == null || chapter.getId() == null || StringUtils.isBlank(chapter.getVideoUrl())) {
            return;
        }
        try {
            videoTimelineAnalysisService.startAutoAnalysis(chapter.getId(), loginUser);
        } catch (Exception e) {
            log.warn("自动生成选集知识点时间轴失败，chapterId={}", chapter.getId(), e);
        }
    }
}
