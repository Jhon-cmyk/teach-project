package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.LearningHeatmapDayVO;
import com.ruyi.teach.service.LearningEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/learning")
@Tag(name = "Student learning data")
public class LearningHeatmapController {

    @Resource
    private LearningEventService learningEventService;

    @Operation(summary = "Current student's learning heatmap")
    @GetMapping("/heatmap")
    public BaseResponse<List<LearningHeatmapDayVO>> getHeatmap(
            @RequestParam(defaultValue = "180") Integer days,
            HttpServletRequest httpRequest) {
        return ResultUtils.success(learningEventService.getLearningHeatmap(getLoginUser(httpRequest), days));
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "Please log in first");
        }
        return loginUser;
    }
}
