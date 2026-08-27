package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionDetailMapper;
import com.ruyi.teach.mapper.UserMapper;
import com.ruyi.teach.model.dto.ExamGradeRequest;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.*;
import com.ruyi.teach.service.HomeworkAssignmentService;
import com.ruyi.teach.service.HomeworkSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exam")
@Tag(name = "考试管理")
public class ExamController {


    @Resource
    private HomeworkAssignmentService assignmentService;

    @Resource
    private HomeworkSubmissionService submissionService;

    @Resource
    private HomeworkSubmissionDetailMapper detailMapper;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private UserMapper userMapper;

    @Operation(summary = "学生查询待考列表")
    @GetMapping("/student/pending")
    public BaseResponse<List<HomeworkPendingVO>> getStudentPending(HttpServletRequest request) {
        User loginUser = getLoginUser(request);

        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可查看");
        }
        if (loginUser.getClassId() == null) {
            return ResultUtils.success(Collections.emptyList());
        }

        Date now = new Date();
        LambdaQueryWrapper<HomeworkAssignment> aw = new LambdaQueryWrapper<>();
        aw.eq(HomeworkAssignment::getClassId, loginUser.getClassId())
                .eq(HomeworkAssignment::getAssignmentType, "exam")
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
        Set<Long> submittedIds = submissions.stream()
                .map(HomeworkSubmission::getAssignmentId)
                .collect(Collectors.toSet());

        List<HomeworkPendingVO> result = new ArrayList<>();
        for (HomeworkAssignment a : assignments) {
            if (submittedIds.contains(a.getId())) {
                continue;
            }

            HomeworkPendingVO vo = new HomeworkPendingVO();
            vo.setAssignmentId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setTeacherNote(a.getTeacherNote());
            vo.setDeadline(a.getDeadline());
            vo.setQuestionCount(a.getQuestionCount());
            vo.setDurationMinutes(a.getDurationMinutes());
            vo.setAttemptCount(0);
            vo.setMaxAttemptCount(1);
            vo.setAllowRedo(0);
            result.add(vo);
        }

        return ResultUtils.success(result);
    }

    @Operation(summary = "学生提交考试（AI自动批阅）")
    @PostMapping("/submission/submit")
    public BaseResponse<Long> submitExam(@RequestBody HomeworkSubmitRequest req,
                                          HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        Long submissionId = submissionService.submitExam(req, loginUser);
        return ResultUtils.success(submissionId);
    }

    @Operation(summary = "学生考试历史")
    @GetMapping("/student/history")
    public BaseResponse<List<HomeworkHistoryVO>> getStudentHistory(HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(submissionService.getExamHistory(loginUser));
    }

    @Operation(summary = "学生查看考试报告")
    @GetMapping("/student/report")
    public BaseResponse<HomeworkReportVO> getStudentReport(@RequestParam Long submissionId,
                                                            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        HomeworkReportVO report = submissionService.getStudentReport(submissionId, loginUser);
        report.setExamMode(true);
        return ResultUtils.success(report);
    }

    @Operation(summary = "教师获取某考试全部学生答卷")
    @GetMapping("/teacher/submissions")
    public BaseResponse<List<ExamTeacherSubmissionVO>> getTeacherSubmissions(@RequestParam Long assignmentId,
                                                                              HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可查看");
        }

        HomeworkAssignment assignment = assignmentService.getById(assignmentId);
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "考试不存在");
        }
        if (!Objects.equals(assignment.getTeacherId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该考试");
        }

        LambdaQueryWrapper<HomeworkSubmission> sw = new LambdaQueryWrapper<>();
        sw.eq(HomeworkSubmission::getAssignmentId, assignmentId)
                .eq(HomeworkSubmission::getIsDelete, 0)
                .orderByAsc(HomeworkSubmission::getStudentId);

        List<HomeworkSubmission> submissions = submissionService.list(sw);
        if (submissions.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }

        List<ExamTeacherSubmissionVO> result = new ArrayList<>();
        for (HomeworkSubmission s : submissions) {
            ExamTeacherSubmissionVO vo = new ExamTeacherSubmissionVO();
            vo.setSubmissionId(s.getId());
            vo.setStudentId(s.getStudentId());
            // 从数据库查询真实学生姓名
            User studentUser = userMapper.selectById(s.getStudentId());
            vo.setStudentName(studentUser != null && studentUser.getUserName() != null
                    ? studentUser.getUserName()
                    : "学生 " + s.getStudentId());
            vo.setSubmitStatus(s.getSubmitStatus());
            vo.setSubmitTime(s.getSubmitTime());
            vo.setTotalScore(s.getTotalScore());
            vo.setTeacherRemark(s.getTeacherRemark());
            vo.setStudentAnswerJson(s.getStudentAnswerJson());
            vo.setContentSnapshot(assignment.getContentSnapshot());

            // 加载每题详情
            LambdaQueryWrapper<HomeworkSubmissionDetail> dw = new LambdaQueryWrapper<>();
            dw.eq(HomeworkSubmissionDetail::getSubmissionId, s.getId())
                    .orderByAsc(HomeworkSubmissionDetail::getQuestionNo);
            List<HomeworkSubmissionDetail> details = detailMapper.selectList(dw);

            List<ExamTeacherSubmissionVO.ExamQuestionDetailVO> detailVOs = details.stream().map(d -> {
                ExamTeacherSubmissionVO.ExamQuestionDetailVO dv = new ExamTeacherSubmissionVO.ExamQuestionDetailVO();
                dv.setId(d.getId());
                dv.setQuestionNo(d.getQuestionNo());
                dv.setQuestionType(d.getQuestionType());
                dv.setStemSnapshot(d.getStemSnapshot());
                dv.setStandardAnswer(d.getStandardAnswer());
                dv.setStudentAnswer(d.getStudentAnswer());
                dv.setImageUrlsJson(d.getImageUrlsJson());
                dv.setFullScore(d.getFullScore());
                dv.setScore(d.getScore());
                dv.setAiComment(d.getAiComment());
                return dv;
            }).collect(Collectors.toList());

            vo.setDetails(detailVOs);
            result.add(vo);
        }

        return ResultUtils.success(result);
    }

    @Operation(summary = "教师批阅考试提交")
    @PostMapping("/teacher/grade")
    public BaseResponse<Boolean> teacherGradeExam(@RequestBody ExamGradeRequest req,
                                                   HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        submissionService.teacherGradeExam(req, loginUser);
        return ResultUtils.success(true);
    }

    @Operation(summary = "教师生成考试批阅评语")
    @PostMapping("/teacher/comment-generate")
    public BaseResponse<String> generateExamReviewComment(@RequestBody ExamGradeRequest req,
                                                          HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        return ResultUtils.success(submissionService.generateExamReviewComment(req, loginUser));
    }

    @Operation(summary = "教师AI自动批阅单个学生答卷")
    @PostMapping("/teacher/auto-grade/{submissionId}")
    public BaseResponse<Boolean> autoGradeSubmission(@PathVariable Long submissionId,
                                                      HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        submissionService.teacherAutoGradeExam(submissionId, loginUser);
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
}
