package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.dto.learning.StudentLearningContextRequest;
import com.ruyi.teach.model.vo.StudentLearningProfileVO;
import com.ruyi.teach.model.vo.StudentLearningContextVO;
import com.ruyi.teach.service.RecommendationService;
import com.ruyi.teach.service.StudentLearningContextService;
import com.ruyi.teach.service.StudentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
@Tag(name = "学生端学习诊断")
public class StudentDiagnosisController {

    @Resource
    private StudentProfileService studentProfileService;

    @Resource
    private RecommendationService recommendationService;

    @Resource
    private StudentLearningContextService studentLearningContextService;

    @Operation(summary = "学生查看自己的综合学习诊断")
    @GetMapping("/learning-profile")
    public BaseResponse<StudentLearningProfileVO> getSelfProfile(
            @RequestParam(value = "days", required = false) Integer days,
            HttpServletRequest request
    ) {
        return ResultUtils.success(studentProfileService.getSelfLearningProfile(days, null, null, getLoginUser(request)));
    }

    @Operation(summary = "学生查看当前课程或章节学习诊断")
    @GetMapping("/course-learning-profile")
    public BaseResponse<StudentLearningProfileVO> getCourseProfile(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "chapterId", required = false) Long chapterId,
            @RequestParam(value = "days", required = false) Integer days,
            HttpServletRequest request
    ) {
        return ResultUtils.success(studentProfileService.getSelfLearningProfile(days, courseId, chapterId, getLoginUser(request)));
    }

    @Operation(summary = "查询学生长期学习背景")
    @GetMapping("/learning-profile/context")
    public BaseResponse<StudentLearningContextVO> getLearningContext(HttpServletRequest request) {
        return ResultUtils.success(studentLearningContextService.getContext(getLoginUser(request)));
    }

    @Operation(summary = "更新学生长期学习背景")
    @PostMapping("/learning-profile/context")
    public BaseResponse<StudentLearningContextVO> updateLearningContext(
            @Valid @RequestBody StudentLearningContextRequest contextRequest,
            HttpServletRequest request) {
        return ResultUtils.success(studentLearningContextService.updateContext(contextRequest, getLoginUser(request)));
    }

    @Operation(summary = "学生标记补强任务已完成")
    @PostMapping("/recommendations/{id}/complete")
    public BaseResponse<Boolean> completeRecommendation(@PathVariable("id") Long id,
                                                        HttpServletRequest request) {
        return ResultUtils.success(recommendationService.updateRecommendationStatus(id, "completed", getLoginUser(request)));
    }

    @Operation(summary = "学生恢复补强任务为待完成")
    @PostMapping("/recommendations/{id}/reopen")
    public BaseResponse<Boolean> reopenRecommendation(@PathVariable("id") Long id,
                                                      HttpServletRequest request) {
        return ResultUtils.success(recommendationService.updateRecommendationStatus(id, "pending", getLoginUser(request)));
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        return loginUser;
    }
}
