package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.AvatarSessionVO;
import com.ruyi.teach.service.AvatarSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avatar")
@RequiredArgsConstructor
@Tag(name = "讯飞数字人")
public class AvatarController {

    private final AvatarSessionService avatarSessionService;

    @GetMapping("/session")
    @Operation(summary = "获取当前登录用户的数字人临时鉴权配置")
    public BaseResponse<AvatarSessionVO> createSession(HttpServletRequest request,
                                                       HttpServletResponse response) {
        User user = SessionUserContext.require(request);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        return ResultUtils.success(avatarSessionService.createSession(user));
    }
}
