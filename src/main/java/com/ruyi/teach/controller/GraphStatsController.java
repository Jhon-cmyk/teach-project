package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.GraphStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher/graph-stats")
@Tag(name = "教师端知识图谱统计")
public class GraphStatsController {

    @Resource
    private GraphStatsService graphStatsService;

    @Operation(summary = "班级学情总览")
    @GetMapping("/overview")
    public BaseResponse<Map<String, Object>> getOverview(
            @RequestParam(value = "classId", required = false) Long classId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);
        return ResultUtils.success(graphStatsService.getOverview(loginUser.getId(), classId));
    }

    @Operation(summary = "班级对比数据")
    @GetMapping("/compare")
    public BaseResponse<Map<String, Object>> getCompare(
            @RequestParam("classA") Long classA,
            @RequestParam("classB") Long classB,
            HttpServletRequest request
    ) {
        getLoginTeacher(request);
        if (classA == null || classB == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "班级ID不能为空");
        }
        return ResultUtils.success(graphStatsService.getCompare(classA, classB));
    }

    @Operation(summary = "学生画像数据")
    @GetMapping("/student-profile")
    public BaseResponse<Map<String, Object>> getStudentProfile(
            @RequestParam(value = "classId", required = false) Long classId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);
        return ResultUtils.success(graphStatsService.getStudentProfile(loginUser.getId(), classId));
    }

    @Operation(summary = "知识点建设统计")
    @GetMapping("/build")
    public BaseResponse<Map<String, Object>> getBuildStats(HttpServletRequest request) {
        getLoginTeacher(request);
        return ResultUtils.success(graphStatsService.getBuildStats());
    }

    @Operation(summary = "获取教师的作业和编程题列表（用于绑定）")
    @GetMapping("/activities/candidates")
    public BaseResponse<Map<String, List<Map<String, Object>>>> getActivityCandidates(
            @RequestParam(value = "courseId", required = false) Long courseId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);
        return ResultUtils.success(graphStatsService.getActivityCandidates(loginUser.getId(), courseId));
    }

    private User getLoginTeacher(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"teacher".equals(loginUser.getUserRole()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师或管理员可访问");
        }
        return loginUser;
    }
}
