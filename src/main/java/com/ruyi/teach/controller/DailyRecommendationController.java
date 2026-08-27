package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.learning.DailyRecommendationSubmitRequest;
import com.ruyi.teach.model.dto.learning.DailyRecommendationInterviewRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.DailyRecommendationTodayVO;
import com.ruyi.teach.model.vo.DailyRecommendationInterviewVO;
import com.ruyi.teach.service.DailyRecommendationService;
import com.ruyi.teach.service.DailyRecommendationInterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student/daily-recommendation")
@Tag(name = "学生每日个性化资源推荐")
public class DailyRecommendationController {

    @Resource
    private DailyRecommendationService dailyRecommendationService;

    @Resource
    private DailyRecommendationInterviewService dailyRecommendationInterviewService;

    @Operation(summary = "查询今日推荐弹窗与推荐结果")
    @GetMapping("/today")
    public BaseResponse<DailyRecommendationTodayVO> today(HttpServletRequest request) {
        return ResultUtils.success(dailyRecommendationService.getToday(getLoginUser(request)));
    }

    @Operation(summary = "只读取今日已缓存推荐结果，不触发推荐生成")
    @GetMapping("/today-cached")
    public BaseResponse<DailyRecommendationTodayVO> todayCached(HttpServletRequest request) {
        return ResultUtils.success(dailyRecommendationService.getTodayCached(getLoginUser(request)));
    }

    @Operation(summary = "今日暂不开启个性化推荐")
    @PostMapping("/dismiss")
    public BaseResponse<DailyRecommendationTodayVO> dismiss(HttpServletRequest request) {
        return ResultUtils.success(dailyRecommendationService.dismissToday(getLoginUser(request)));
    }

    @Operation(summary = "提交今日问答并生成资源推荐")
    @PostMapping("/submit")
    public BaseResponse<DailyRecommendationTodayVO> submit(@RequestBody DailyRecommendationSubmitRequest submitRequest,
                                                          HttpServletRequest request) {
        return ResultUtils.success(dailyRecommendationService.submitToday(submitRequest, getLoginUser(request)));
    }

    @Operation(summary = "AI 对话采集今日学习需求")
    @PostMapping("/interview")
    public BaseResponse<DailyRecommendationInterviewVO> interview(
            @RequestBody DailyRecommendationInterviewRequest interviewRequest,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可使用学习访谈");
        }
        return ResultUtils.success(dailyRecommendationInterviewService.interview(interviewRequest));
    }

    @Operation(summary = "鍒锋柊浠婃棩琛屼负鏁版嵁鎺ㄨ崘")
    @PostMapping("/refresh")
    public BaseResponse<DailyRecommendationTodayVO> refresh(HttpServletRequest request) {
        return ResultUtils.success(dailyRecommendationService.refreshToday(getLoginUser(request)));
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        return loginUser;
    }
}
