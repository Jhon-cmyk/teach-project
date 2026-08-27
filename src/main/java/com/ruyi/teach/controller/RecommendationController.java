package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.learning.RecommendationGenerateRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.RecommendationGenerateVO;
import com.ruyi.teach.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
@Tag(name = "个性化学习资源推荐")
public class RecommendationController {

    @Resource
    private RecommendationService recommendationService;

    @Operation(summary = "生成学生个性化资源推荐")
    @PostMapping("/generate")
    public BaseResponse<RecommendationGenerateVO> generate(@RequestBody RecommendationGenerateRequest request,
                                                           HttpServletRequest httpRequest) {
        return ResultUtils.success(recommendationService.generateRecommendations(request, getLoginUser(httpRequest)));
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        return loginUser;
    }
}
