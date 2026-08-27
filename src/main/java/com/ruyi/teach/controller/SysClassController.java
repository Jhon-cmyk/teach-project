package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.entity.FatigueRecord;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.SysClass;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.StudentTrajectoryVO;
import com.ruyi.teach.model.vo.StudentVO;
import com.ruyi.teach.service.FatigueRecordService;
import com.ruyi.teach.service.HomeworkAssignmentService;
import com.ruyi.teach.service.HomeworkSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/class")
@Tag(name = "班级管理")
public class SysClassController {


    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private HomeworkAssignmentService homeworkAssignmentService;

    @Resource
    private HomeworkSubmissionService homeworkSubmissionService;

    @Resource
    private FatigueRecordService fatigueRecordService;

    @Operation(summary = "获取所有班级列表(带人数)")
    @GetMapping("/list")
    public BaseResponse<List<SysClass>> listClasses() {
        List<SysClass> list = sysClassMapper.selectAllClassesWithStudentCount();
        return ResultUtils.success(list);
    }

    @Operation(summary = "获取当前教师带的班级列表")
    @GetMapping("/my-classes")
    public BaseResponse<List<SysClass>> listMyClasses(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"teacher".equals(loginUser.getUserRole()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师或管理员可查看");
        }

        List<SysClass> classList = sysClassMapper.selectMyClasses(loginUser.getId());
        return ResultUtils.success(classList);
    }


    @GetMapping("/my-teaching-classes")
    public BaseResponse<List<SysClass>> listMyTeachingClasses(@RequestParam(required = false) String semesterLabel,
                                                              HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"teacher".equals(loginUser.getUserRole()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师或管理员可查看");
        }

        List<SysClass> classList = sysClassMapper.selectTeachingClassesBySchedule(
                loginUser.getId(),
                normalizeSemesterLabel(semesterLabel)
        );
        return ResultUtils.success(classList);
    }

    @Operation(summary = "获取班级下的学生名单")
    @GetMapping("/students")
    public BaseResponse<List<StudentVO>> getClassStudents(@RequestParam("classId") Long classId) {
        List<StudentVO> studentList = sysClassMapper.getStudentsByClassId(classId);
        return ResultUtils.success(studentList);
    }

    @Operation(summary = "教师查看学生学习轨迹")
    @GetMapping("/student/trajectory")
    public BaseResponse<StudentTrajectoryVO> getStudentTrajectory(@RequestParam("classId") Long classId,
                                                                  @RequestParam("studentId") Long studentId,
                                                                  HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"teacher".equals(loginUser.getUserRole()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师或管理员可查看");
        }
        if (classId == null || studentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "classId和studentId不能为空");
        }

        // 先校验：这个学生是否真的属于当前班级
        List<StudentVO> studentList = sysClassMapper.getStudentsByClassId(classId);
        StudentVO targetStudent = studentList.stream()
                .filter(item -> Objects.equals(item.getId(), studentId))
                .findFirst()
                .orElse(null);

        if (targetStudent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学生不存在或不属于当前班级");
        }

        SysClass sysClass = sysClassMapper.selectById(classId);

        StudentTrajectoryVO vo = new StudentTrajectoryVO();
        vo.setClassId(classId);
        vo.setClassName(sysClass != null ? sysClass.getName() : ("班级 " + classId));
        vo.setStudentId(targetStudent.getId());
        vo.setStudentNo(targetStudent.getStudentNo());
        vo.setStudentName(targetStudent.getName());

        // =========================
        // 1. 作业完成情况 / 未完成作业
        // =========================
        LambdaQueryWrapper<HomeworkAssignment> aw = new LambdaQueryWrapper<>();
        aw.eq(HomeworkAssignment::getClassId, classId)
                .eq(HomeworkAssignment::getAssignmentType, "homework")
                .eq(HomeworkAssignment::getStatus, "published")
                .eq(HomeworkAssignment::getIsDelete, 0)
                .orderByDesc(HomeworkAssignment::getCreateTime);

        List<HomeworkAssignment> assignmentList = homeworkAssignmentService.list(aw);

        Set<Long> completedAssignmentIds = new HashSet<>();

        if (!assignmentList.isEmpty()) {
            List<Long> assignmentIds = assignmentList.stream()
                    .map(HomeworkAssignment::getId)
                    .distinct()
                    .collect(Collectors.toList());

            LambdaQueryWrapper<HomeworkSubmission> sw = new LambdaQueryWrapper<>();
            sw.eq(HomeworkSubmission::getStudentId, studentId)
                    .in(HomeworkSubmission::getAssignmentId, assignmentIds)
                    .eq(HomeworkSubmission::getIsDelete, 0);

            List<HomeworkSubmission> submissionList = homeworkSubmissionService.list(sw);

            completedAssignmentIds.addAll(
                    submissionList.stream()
                            .filter(item -> "completed".equals(item.getSubmitStatus()))
                            .map(HomeworkSubmission::getAssignmentId)
                            .collect(Collectors.toSet())
            );
        }

        List<StudentTrajectoryVO.UnfinishedHomeworkItem> unfinishedHomeworkList = assignmentList.stream()
                .filter(item -> !completedAssignmentIds.contains(item.getId()))
                .map(item -> {
                    StudentTrajectoryVO.UnfinishedHomeworkItem hw = new StudentTrajectoryVO.UnfinishedHomeworkItem();
                    hw.setAssignmentId(item.getId());
                    hw.setTitle(item.getTitle());
                    hw.setDeadline(item.getDeadline());
                    hw.setQuestionCount(item.getQuestionCount());
                    hw.setTotalScore(item.getTotalScore());
                    hw.setAssignmentType(item.getAssignmentType());
                    return hw;
                })
                .collect(Collectors.toList());

        StudentTrajectoryVO.Summary summary = new StudentTrajectoryVO.Summary();
        int totalHomeworkCount = assignmentList.size();
        int completedHomeworkCount = completedAssignmentIds.size();
        int unfinishedHomeworkCount = unfinishedHomeworkList.size();

        summary.setTotalHomeworkCount(totalHomeworkCount);
        summary.setCompletedHomeworkCount(completedHomeworkCount);
        summary.setUnfinishedHomeworkCount(unfinishedHomeworkCount);
        summary.setHasUnfinishedHomework(unfinishedHomeworkCount > 0);
        summary.setCompletionRate(totalHomeworkCount == 0 ? 0 : (int) Math.round(completedHomeworkCount * 100.0 / totalHomeworkCount));

        // =========================
// 2. 今日学习时长
// =========================
        FatigueRecord todayRecord = fatigueRecordService.getByUserAndDate(studentId, LocalDate.now());

        int todayStudySeconds = 0;
        if (todayRecord != null && todayRecord.getMonitorSeconds() != null) {
            todayStudySeconds = todayRecord.getMonitorSeconds();
        }

        summary.setTotalStudySeconds(todayStudySeconds);
        summary.setTotalStudyDurationText(formatDuration(todayStudySeconds));

        vo.setSummary(summary);
        vo.setUnfinishedHomeworkList(unfinishedHomeworkList);

        // =========================
        // 3. 疲劳检测结果（取最近一次）
        // =========================
        LambdaQueryWrapper<FatigueRecord> latestFatigueWrapper = new LambdaQueryWrapper<>();
        latestFatigueWrapper.eq(FatigueRecord::getUserId, studentId)
                .orderByDesc(FatigueRecord::getRecordDate)
                .orderByDesc(FatigueRecord::getId)
                .last("limit 1");

        FatigueRecord latestRecord = fatigueRecordService.getOne(latestFatigueWrapper, false);

        StudentTrajectoryVO.Fatigue fatigue = new StudentTrajectoryVO.Fatigue();
        if (latestRecord != null) {
            fatigue.setLatestRecordDate(latestRecord.getRecordDate() == null ? "" : latestRecord.getRecordDate().toString());
            fatigue.setFatigueCount(defaultZero(latestRecord.getFatigueCount()));
            fatigue.setYawnCount(defaultZero(latestRecord.getYawnCount()));
            fatigue.setNoFaceCount(defaultZero(latestRecord.getNoFaceCount()));
            fatigue.setNormalCount(defaultZero(latestRecord.getNormalCount()));
            fatigue.setTotalDetections(defaultZero(latestRecord.getTotalDetections()));
            fatigue.setMonitorSeconds(defaultZero(latestRecord.getMonitorSeconds()));
            fatigue.setLastStatus(latestRecord.getLastStatus());
            fatigue.setLastStatusText(resolveStatusText(latestRecord.getLastStatus()));
            fatigue.setFatigueLevelText(resolveFatigueLevel(latestRecord));
        } else {
            fatigue.setLatestRecordDate("");
            fatigue.setFatigueCount(0);
            fatigue.setYawnCount(0);
            fatigue.setNoFaceCount(0);
            fatigue.setNormalCount(0);
            fatigue.setTotalDetections(0);
            fatigue.setMonitorSeconds(0);
            fatigue.setLastStatus("");
            fatigue.setLastStatusText("暂无记录");
            fatigue.setFatigueLevelText("暂无记录");
        }

        vo.setFatigue(fatigue);

        return ResultUtils.success(vo);
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }


    private String normalizeSemesterLabel(String semesterLabel) {
        if (semesterLabel != null && !semesterLabel.trim().isEmpty()) {
            return semesterLabel.trim();
        }
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        if (month >= 9) {
            return year + "-" + (year + 1) + "-1";
        }
        return (year - 1) + "-" + year + "-2";
    }

    private String formatDuration(Integer seconds) {
        int safeSeconds = seconds == null ? 0 : seconds;
        int hours = safeSeconds / 3600;
        int minutes = (safeSeconds % 3600) / 60;

        if (hours <= 0 && minutes <= 0) {
            return "0分钟";
        }
        if (hours <= 0) {
            return minutes + "分钟";
        }
        if (minutes <= 0) {
            return hours + "小时";
        }
        return hours + "小时" + minutes + "分钟";
    }

    private String resolveStatusText(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "暂无记录";
        }
        switch (status.toLowerCase()) {
            case "normal":
                return "正常";
            case "fatigue":
                return "疲劳";
            case "yawn":
                return "打哈欠";
            case "no_face":
                return "离屏";
            default:
                return status;
        }
    }

    private String resolveFatigueLevel(FatigueRecord record) {
        if (record == null) {
            return "暂无记录";
        }

        String status = record.getLastStatus() == null ? "" : record.getLastStatus().toLowerCase();
        int fatigueCount = defaultZero(record.getFatigueCount());
        int yawnCount = defaultZero(record.getYawnCount());
        int noFaceCount = defaultZero(record.getNoFaceCount());

        if ("fatigue".equals(status) || fatigueCount >= 8) {
            return "疲劳较明显";
        }
        if ("yawn".equals(status) || yawnCount >= 3 || fatigueCount >= 3) {
            return "轻度疲劳";
        }
        if ("no_face".equals(status) || noFaceCount >= 3) {
            return "离屏较多";
        }
        return "状态正常";
    }
}
