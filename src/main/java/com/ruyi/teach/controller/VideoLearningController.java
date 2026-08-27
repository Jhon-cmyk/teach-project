package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.video.VideoInterventionCheckRequest;
import com.ruyi.teach.model.dto.video.VideoLearningEventBatchRequest;
import com.ruyi.teach.model.dto.video.VideoLearningSessionStartRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.VideoInterventionVO;
import com.ruyi.teach.model.vo.VideoLearningProfileVO;
import com.ruyi.teach.service.VideoLearningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/video-learning")
@Tag(name = "学生视频学习行为分析")
public class VideoLearningController {


    @Resource
    private VideoLearningService videoLearningService;

    @Operation(summary = "学生开始视频学习会话")
    @PostMapping("/session/start")
    public BaseResponse<Long> startSession(@RequestBody VideoLearningSessionStartRequest request,
                                           HttpServletRequest httpRequest) {
        return ResultUtils.success(videoLearningService.startSession(request, getLoginUser(httpRequest)));
    }

    @Operation(summary = "批量上报视频学习行为事件")
    @PostMapping("/events/batch")
    public BaseResponse<Boolean> saveEvents(@RequestBody VideoLearningEventBatchRequest request,
                                            HttpServletRequest httpRequest) {
        return ResultUtils.success(videoLearningService.saveEvents(request, getLoginUser(httpRequest)));
    }

    @Operation(summary = "检查是否需要触发实时辅导")
    @PostMapping("/intervention/check")
    public BaseResponse<VideoInterventionVO> checkIntervention(@RequestBody VideoInterventionCheckRequest request,
                                                               HttpServletRequest httpRequest) {
        return ResultUtils.success(videoLearningService.checkIntervention(request, getLoginUser(httpRequest)));
    }

    @Operation(summary = "老师查看学生视频学习行为画像")
    @GetMapping("/student/profile")
    public BaseResponse<VideoLearningProfileVO> getStudentProfile(@RequestParam("classId") Long classId,
                                                                  @RequestParam("studentId") Long studentId,
                                                                  @RequestParam(value = "days", required = false) Integer days,
                                                                  HttpServletRequest httpRequest) {
        User loginUser = getLoginUser(httpRequest);
        if (!"teacher".equals(loginUser.getUserRole()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师或管理员可查看学生视频画像");
        }
        return ResultUtils.success(videoLearningService.getStudentProfile(
                loginUser.getId(),
                loginUser.getUserRole(),
                classId,
                studentId,
                days
        ));
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        return loginUser;
    }
}
