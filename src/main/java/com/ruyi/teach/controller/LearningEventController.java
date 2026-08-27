package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.learning.LearningEventBatchRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.LearningEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/learning-events")
@Tag(name = "通用学习行为采集")
public class LearningEventController {

    @Resource
    private LearningEventService learningEventService;

    @Operation(summary = "学生批量上报学习行为")
    @PostMapping("/batch")
    public BaseResponse<Boolean> saveBatch(@RequestBody LearningEventBatchRequest request,
                                           HttpServletRequest httpRequest) {
        return ResultUtils.success(learningEventService.saveBatchEvents(request, getLoginUser(httpRequest)));
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        return loginUser;
    }
}
