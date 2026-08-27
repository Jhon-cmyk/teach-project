package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.model.dto.HomeworkChapterPracticePublishRequest;
import com.ruyi.teach.model.dto.HomeworkPublishRequest;
import com.ruyi.teach.model.dto.HomeworkSubmissionReviewRequest;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.dto.HomeworkTeacherMonitorReportRequest;
import com.ruyi.teach.model.dto.PersonalPracticeCreateRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.*;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.HomeworkAssignmentService;
import com.ruyi.teach.service.HomeworkMonitorService;
import com.ruyi.teach.service.HomeworkSubmissionService;
import com.ruyi.teach.service.OssService;
import com.ruyi.teach.service.PersonalPracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/homework")
@Tag(name = "作业管理")
@Slf4j
public class HomeworkController {


    @Resource
    private HomeworkAssignmentService assignmentService;

    @Resource
    private HomeworkSubmissionService submissionService;

    @Resource
    private HomeworkMonitorService homeworkMonitorService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private OssService ossService;

    @Resource
    private PersonalPracticeService personalPracticeService;

    @Operation(summary = "学生创建个性化专项练习")
    @PostMapping("/student/personal-practice/create")
    public BaseResponse<PersonalPracticeCreateVO> createPersonalPractice(
            @Valid @RequestBody PersonalPracticeCreateRequest req,
            HttpServletRequest request) {
        return ResultUtils.success(personalPracticeService.create(req, getLoginUser(request)));
    }

    @Operation(summary = "教师发布正式作业")
    @PostMapping("/assignment/publish")
    public BaseResponse<Long> publishAssignment(@Valid @RequestBody HomeworkPublishRequest req,
                                                HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        Long assignmentId = assignmentService.publishAssignment(req, loginUser);
        return ResultUtils.success(assignmentId);
    }

    @Operation(summary = "教师发布章节随堂练习")
    @PostMapping("/practice/publish")
    public BaseResponse<Long> publishChapterPractice(@Valid @RequestBody HomeworkChapterPracticePublishRequest req,
                                                     HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        Long assignmentId = assignmentService.publishChapterPractice(req, loginUser);
        return ResultUtils.success(assignmentId);
    }

    @Operation(summary = "教师获取课程章节练习发布元数据")
    @GetMapping("/teacher/course-practice-meta")
    public BaseResponse<HomeworkCoursePublishMetaVO> getCoursePracticeMeta(@RequestParam Long courseId,
                                                                           HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可操作");
        }
        if (courseId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "courseId不能为空");
        }

        LambdaQueryWrapper<CourseChapter> chapterWrapper = new LambdaQueryWrapper<>();
        chapterWrapper.eq(CourseChapter::getCourseId, courseId)
                .eq(CourseChapter::getIsDelete, 0)
                .orderByAsc(CourseChapter::getSortOrder)
                .orderByAsc(CourseChapter::getId);
        List<CourseChapter> chapterList = courseChapterService.list(chapterWrapper);

        List<Long> classIds = courseMapper.selectClassIdsByCourseId(courseId);

        LambdaQueryWrapper<AiResource> quizWrapper = new LambdaQueryWrapper<>();
        quizWrapper.eq(AiResource::getTeacherId, loginUser.getId())
                .eq(AiResource::getType, "quiz")
                .eq(AiResource::getIsDelete, 0)
                .orderByDesc(AiResource::getCreateTime);
        List<AiResource> quizList = aiResourceMapper.selectList(quizWrapper);

        HomeworkCoursePublishMetaVO vo = new HomeworkCoursePublishMetaVO();

        vo.setChapterList(chapterList.stream().map(chapter -> {
            HomeworkPublishChapterOptionVO item = new HomeworkPublishChapterOptionVO();
            item.setChapterId(chapter.getId());
            item.setChapterTitle(chapter.getTitle());
            item.setSortOrder(chapter.getSortOrder());
            return item;
        }).collect(Collectors.toList()));

        vo.setClassList((classIds == null ? Collections.<Long>emptyList() : classIds).stream().map(classId -> {
            HomeworkPublishClassOptionVO item = new HomeworkPublishClassOptionVO();
            item.setClassId(classId);
            item.setClassName("班级 " + classId);
            return item;
        }).collect(Collectors.toList()));

        vo.setQuizList(quizList.stream().map(quiz -> {
            HomeworkPublishQuizOptionVO item = new HomeworkPublishQuizOptionVO();
            item.setQuizResourceId(quiz.getId());
            item.setTitle(quiz.getTitle());
            item.setCreateTime(quiz.getCreateTime());
            return item;
        }).collect(Collectors.toList()));

        return ResultUtils.success(vo);
    }

    @Operation(summary = "教师作业学情列表")
    @GetMapping("/teacher/monitor/list")
    public BaseResponse<List<HomeworkTeacherMonitorItemVO>> getTeacherMonitorList(HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(homeworkMonitorService.listTeacherMonitor(loginUser));
    }

    @Operation(summary = "教师查看单个作业学生详情")
    @GetMapping("/teacher/monitor/detail")
    public BaseResponse<List<HomeworkTeacherMonitorStudentVO>> getTeacherMonitorDetail(@RequestParam Long assignmentId,
                                                                                       HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(homeworkMonitorService.getTeacherMonitorDetail(assignmentId, loginUser));
    }

    @Operation(summary = "教师生成并保存学情诊断报告")
    @PostMapping("/teacher/monitor/report")
    public BaseResponse<HomeworkTeacherMonitorReportVO> generateTeacherMonitorReport(
            @RequestBody(required = false) HomeworkTeacherMonitorReportRequest reportRequest,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        HomeworkTeacherMonitorReportRequest safeRequest =
                reportRequest == null ? new HomeworkTeacherMonitorReportRequest() : reportRequest;
        return ResultUtils.success(homeworkMonitorService.generateTeacherMonitorReport(safeRequest, loginUser));
    }

    @Operation(summary = "教师查看学情报告历史")
    @GetMapping("/teacher/monitor/report/history")
    public BaseResponse<List<HomeworkTeacherMonitorReportVO>> getTeacherMonitorReportHistory(HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(homeworkMonitorService.listTeacherMonitorReportHistory(loginUser));
    }

    @Operation(summary = "教师查看单条学情报告详情")
    @GetMapping("/teacher/monitor/report/read")
    public BaseResponse<HomeworkTeacherMonitorReportVO> getTeacherMonitorReportDetail(@RequestParam Long reportId,
                                                                                      HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(homeworkMonitorService.getTeacherMonitorReportDetail(reportId, loginUser));
    }

    @Operation(summary = "学生查询待完成正式作业")
    @GetMapping("/student/pending")
    public BaseResponse<List<HomeworkPendingVO>> getStudentPending(HttpServletRequest request) {
        User loginUser = getLoginUser(request);

        if (loginUser.getClassId() == null) {
            return ResultUtils.success(Collections.emptyList());
        }

        Date now = new Date();
        LambdaQueryWrapper<HomeworkAssignment> aw = new LambdaQueryWrapper<>();
        aw.eq(HomeworkAssignment::getClassId, loginUser.getClassId())
                .eq(HomeworkAssignment::getAssignmentType, "homework")
                .eq(HomeworkAssignment::getStatus, "published")
                .eq(HomeworkAssignment::getIsDelete, 0)
                .and(w -> w.isNull(HomeworkAssignment::getDeadline)
                        .or()
                        .ge(HomeworkAssignment::getDeadline, now))
                .orderByDesc(HomeworkAssignment::getCreateTime);

        List<HomeworkAssignment> assignments = assignmentService.list(aw);
        assignments = filterAssignmentsWithActiveQuizResource(assignments);
        if (assignments.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }

        List<Long> assignmentIds = assignments.stream()
                .map(HomeworkAssignment::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<HomeworkSubmission> sw = new LambdaQueryWrapper<>();
        sw.eq(HomeworkSubmission::getStudentId, loginUser.getId())
                .in(HomeworkSubmission::getAssignmentId, assignmentIds)
                .eq(HomeworkSubmission::getIsDelete, 0);

        List<HomeworkSubmission> submissions = submissionService.list(sw);

        Map<Long, List<HomeworkSubmission>> subMap = submissions.stream()
                .collect(Collectors.groupingBy(HomeworkSubmission::getAssignmentId));

        List<HomeworkPendingVO> result = new ArrayList<>();
        for (HomeworkAssignment a : assignments) {
            List<HomeworkSubmission> subs = subMap.getOrDefault(a.getId(), Collections.emptyList());
            int attemptCount = subs.size();

            boolean hasCompleted = subs.stream()
                    .anyMatch(s -> "completed".equals(s.getSubmitStatus()));

            if (hasCompleted) {
                continue;
            }

            if (a.getMaxAttemptCount() != null && attemptCount >= a.getMaxAttemptCount()) {
                continue;
            }

            HomeworkPendingVO vo = new HomeworkPendingVO();
            vo.setAssignmentId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setTeacherNote(a.getTeacherNote());
            vo.setDeadline(a.getDeadline());
            vo.setQuestionCount(a.getQuestionCount());
            vo.setAttemptCount(attemptCount);
            vo.setMaxAttemptCount(a.getMaxAttemptCount());
            vo.setAllowRedo(a.getAllowRedo());
            result.add(vo);
        }

        return ResultUtils.success(result);
    }

    @Operation(summary = "学生读取当前章节随堂练习")
    @GetMapping("/student/course-chapter/practice/current")
    public BaseResponse<HomeworkStudentChapterPracticeVO> getStudentCurrentChapterPractice(@RequestParam Long courseId,
                                                                                           @RequestParam Long chapterId,
                                                                                           HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可查看");
        }
        if (courseId == null || chapterId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "courseId和chapterId不能为空");
        }
        if (loginUser.getClassId() == null) {
            return ResultUtils.success(null);
        }

        LambdaQueryWrapper<HomeworkAssignment> aw = new LambdaQueryWrapper<>();
        aw.eq(HomeworkAssignment::getCourseId, courseId)
                .eq(HomeworkAssignment::getChapterId, chapterId)
                .eq(HomeworkAssignment::getClassId, loginUser.getClassId())
                .eq(HomeworkAssignment::getAssignmentType, "chapter_practice")
                .eq(HomeworkAssignment::getStatus, "published")
                .eq(HomeworkAssignment::getIsDelete, 0)
                .orderByDesc(HomeworkAssignment::getCreateTime)
                .orderByDesc(HomeworkAssignment::getId)
                .last("limit 1");

        HomeworkAssignment assignment = assignmentService.getOne(aw, false);
        if (assignment == null || filterAssignmentsWithActiveQuizResource(List.of(assignment)).isEmpty()) {
            return ResultUtils.success(null);
        }

        LambdaQueryWrapper<HomeworkSubmission> sw = new LambdaQueryWrapper<>();
        sw.eq(HomeworkSubmission::getAssignmentId, assignment.getId())
                .eq(HomeworkSubmission::getStudentId, loginUser.getId())
                .eq(HomeworkSubmission::getIsDelete, 0)
                .orderByDesc(HomeworkSubmission::getCreateTime)
                .orderByDesc(HomeworkSubmission::getId);

        List<HomeworkSubmission> submissions = submissionService.list(sw);
        HomeworkSubmission latestSubmission = submissions.isEmpty() ? null : submissions.get(0);

        HomeworkStudentChapterPracticeVO vo = new HomeworkStudentChapterPracticeVO();
        vo.setAssignmentId(assignment.getId());
        vo.setTitle(assignment.getTitle());
        vo.setCourseId(assignment.getCourseId());
        vo.setChapterId(assignment.getChapterId());
        vo.setChapterTitle(assignment.getChapterTitleSnapshot());
        vo.setQuestionCount(assignment.getQuestionCount());
        vo.setTotalScore(assignment.getTotalScore());
        vo.setAttemptCount(submissions.size());
        vo.setTeacherNote(assignment.getTeacherNote());

        if (latestSubmission == null) {
            vo.setSubmitStatus("not_started");
            vo.setCompleted(false);
            vo.setLatestSubmissionId(null);
            vo.setLatestScore(null);
        } else {
            vo.setSubmitStatus(latestSubmission.getSubmitStatus());
            vo.setCompleted("completed".equals(latestSubmission.getSubmitStatus()));
            vo.setLatestSubmissionId(latestSubmission.getId());
            vo.setLatestScore(latestSubmission.getTotalScore());
        }

        return ResultUtils.success(vo);
    }

    @Operation(summary = "学生查看作业详情")
    @GetMapping("/student/detail")
    public BaseResponse<HomeworkDetailVO> getStudentDetail(@RequestParam Long assignmentId,
                                                           HttpServletRequest request) {
        User loginUser = getLoginUser(request);

        HomeworkAssignment a = assignmentService.getById(assignmentId);
        if (a == null
                || (a.getIsDelete() != null && a.getIsDelete() == 1)
                || filterAssignmentsWithActiveQuizResource(List.of(a)).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业不存在");
        }
        if (a.getTargetStudentId() != null && !Objects.equals(a.getTargetStudentId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该练习不属于当前学生");
        }

        LambdaQueryWrapper<HomeworkSubmission> sw = new LambdaQueryWrapper<>();
        sw.eq(HomeworkSubmission::getAssignmentId, assignmentId)
                .eq(HomeworkSubmission::getStudentId, loginUser.getId())
                .eq(HomeworkSubmission::getIsDelete, 0);
        List<HomeworkSubmission> subs = submissionService.list(sw);

        HomeworkDetailVO vo = new HomeworkDetailVO();
        vo.setAssignmentId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setTeacherNote(a.getTeacherNote());
        vo.setDeadline(a.getDeadline());
        vo.setContentSnapshot(a.getContentSnapshot());
        vo.setParamsSnapshot(a.getParamsSnapshot());
        vo.setAnswerMode(a.getAnswerMode() == null ? "online" : a.getAnswerMode());
        vo.setImageGranularity(a.getImageGranularity() == null ? "both" : a.getImageGranularity());
        vo.setGradingMode(a.getGradingMode() == null ? "auto" : a.getGradingMode());
        vo.setAssignmentType(a.getAssignmentType());
        vo.setSourceType(a.getSourceType());
        vo.setQuestionCount(a.getQuestionCount());
        vo.setTotalScore(a.getTotalScore());
        vo.setAllowRedo(a.getAllowRedo());
        vo.setMaxAttemptCount(a.getMaxAttemptCount());
        vo.setDurationMinutes(a.getDurationMinutes());
        vo.setAttemptCount(subs.size());
        vo.setCompleted(subs.stream().anyMatch(s -> "completed".equals(s.getSubmitStatus())
                || ("personal_practice".equals(a.getAssignmentType()) && "submitted".equals(s.getSubmitStatus()))));
        vo.setLatestSubmissionId(subs.stream()
                .max(Comparator.comparing(HomeworkSubmission::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(HomeworkSubmission::getId)
                .orElse(null));

        return ResultUtils.success(vo);
    }

    @Operation(summary = "学生提交作业/练习")
    @PostMapping("/submission/submit")
    public BaseResponse<Long> submitHomework(@Valid @RequestBody HomeworkSubmitRequest req,
                                             HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        Long submissionId = submissionService.submitHomework(req, loginUser);
        return ResultUtils.success(submissionId);
    }

    @Operation(summary = "学生上传作业图片")
    @PostMapping("/submission/image/upload")
    public BaseResponse<String> uploadHomeworkImage(@RequestParam("file") MultipartFile file,
                                                    HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "student only");
        }
        validateHomeworkImage(file);
        return ResultUtils.success(ossService.uploadFile(file, "homework-submission"));
    }

    @Operation(summary = "学生异步提交作业/练习")
    @PostMapping("/submission/submit-async")
    public BaseResponse<Long> submitHomeworkAsync(@Valid @RequestBody HomeworkSubmitRequest req,
                                                  HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        Long submissionId = submissionService.submitHomeworkAsync(req, loginUser);
        return ResultUtils.success(submissionId);
    }

    @Operation(summary = "学生流式获取批改报告")
    @GetMapping("/submission/stream")
    public void streamSubmissionReport(@RequestParam Long submissionId,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        User loginUser = getLoginUser(request);

        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            response.getWriter().flush();

            submissionService.streamGradeSubmission(submissionId, loginUser, chunk -> {
                try {
                    response.getWriter().write(chunk);
                    response.getWriter().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("Homework report stream failed, submissionId={}", submissionId, e);
            try {
                response.getWriter().write("\n[STREAM_ERROR] 批改报告生成失败，请稍后重试");
                response.getWriter().flush();
            } catch (IOException ignored) {
            }
        }
    }

    @Operation(summary = "学生查询历史作答")
    @GetMapping("/student/history")
    public BaseResponse<List<HomeworkHistoryVO>> getStudentHistory(HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(submissionService.getStudentHistory(loginUser));
    }

    @Operation(summary = "学生删除自己的单条作业记录")
    @DeleteMapping("/student/history/{submissionId}")
    public BaseResponse<Boolean> deleteStudentHistory(@PathVariable Long submissionId,
                                                      HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        submissionService.deleteStudentHomeworkHistory(submissionId, loginUser);
        return ResultUtils.success(true);
    }

    @Operation(summary = "学生查看单次作答报告详情")
    @GetMapping("/student/report")
    public BaseResponse<HomeworkReportVO> getStudentReport(@RequestParam Long submissionId,
                                                           HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(submissionService.getStudentReport(submissionId, loginUser));
    }

    @Operation(summary = "教师查看学生单次作答报告详情")
    @GetMapping("/teacher/monitor/submission-report")
    public BaseResponse<HomeworkReportVO> getTeacherSubmissionReport(@RequestParam Long submissionId,
                                                                     HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(submissionService.getTeacherSubmissionReport(submissionId, loginUser));
    }

    @Operation(summary = "教师保存作业批改")
    @PostMapping("/teacher/submission/review")
    public BaseResponse<Boolean> reviewHomeworkSubmission(@Valid @RequestBody HomeworkSubmissionReviewRequest req,
                                                          HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        submissionService.teacherReviewHomeworkSubmission(req, loginUser);
        return ResultUtils.success(true);
    }

    @Operation(summary = "教师生成作业批改评语")
    @PostMapping("/teacher/submission/comment-generate")
    public BaseResponse<String> generateHomeworkReviewComment(@Valid @RequestBody HomeworkSubmissionReviewRequest req,
                                                              HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(submissionService.generateHomeworkReviewComment(req, loginUser));
    }

    @Operation(summary = "教师重跑作业 AI 批改")
    @PostMapping("/teacher/submission/regrade-ai/{submissionId}")
    public BaseResponse<Boolean> regradeHomeworkSubmission(@PathVariable Long submissionId,
                                                           HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        submissionService.teacherRegradeHomeworkSubmission(submissionId, loginUser);
        return ResultUtils.success(true);
    }

    private List<HomeworkAssignment> filterAssignmentsWithActiveQuizResource(List<HomeworkAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> resourceIds = assignments.stream()
                .map(HomeworkAssignment::getQuizResourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (resourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, AiResource> resourceMap = aiResourceMapper.selectBatchIds(resourceIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AiResource::getId, r -> r, (a, b) -> a));
        return assignments.stream()
                .filter(a -> hasActiveQuizResource(a, resourceMap))
                .collect(Collectors.toList());
    }

    private boolean hasActiveQuizResource(HomeworkAssignment assignment, Map<Long, AiResource> resourceMap) {
        if (assignment == null || assignment.getQuizResourceId() == null || resourceMap == null) {
            return false;
        }
        AiResource resource = resourceMap.get(assignment.getQuizResourceId());
        return resource != null
                && (resource.getIsDelete() == null || resource.getIsDelete() == 0)
                && "quiz".equals(resource.getType());
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return loginUser;
    }

    private void validateHomeworkImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "image cannot be empty");
        }
        if (file.getSize() > 10L * 1024L * 1024L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "image must be <= 10MB");
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        String lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        boolean extOk = lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png") || lowerName.endsWith(".webp");
        boolean typeOk = "image/jpeg".equals(contentType)
                || "image/png".equals(contentType)
                || "image/webp".equals(contentType);
        if (!extOk || !typeOk) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "only jpg, png, and webp images are supported");
        }
    }

    @Operation(summary = "教师删除学情报告")
    @PostMapping("/teacher/monitor/report/delete/{reportId}")
    public BaseResponse<Boolean> deleteTeacherMonitorReport(
            @PathVariable Long reportId,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(
                homeworkMonitorService.deleteTeacherMonitorReport(reportId, loginUser)
        );
    }

    @Operation(summary = "教师删除作业（软删除）")
    @PostMapping("/teacher/monitor/assignment/delete/{assignmentId}")
    public BaseResponse<Boolean> deleteTeacherAssignment(
            @PathVariable Long assignmentId,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(
                homeworkMonitorService.deleteTeacherAssignment(assignmentId, loginUser)
        );
    }
}
