package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.PlatformBannerMapper;
import com.ruyi.teach.model.entity.PlatformBanner;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/banner")
@Tag(name = "管理端广告图管理")
public class AdminBannerController {


    @Resource
    private PlatformBannerMapper platformBannerMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Operation(summary = "广告图列表")
    @GetMapping("/list")
    public BaseResponse<Page<PlatformBanner>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String title,
            HttpServletRequest request) {

        getAdminLoginUser(request);

        QueryWrapper<PlatformBanner> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like("title", title.trim());
        }
        queryWrapper.orderByAsc("sort_order").orderByDesc("create_time");

        Page<PlatformBanner> page = new Page<>(current, size);
        Page<PlatformBanner> result = platformBannerMapper.selectPage(page, queryWrapper);
        return ResultUtils.success(result);
    }

    @Operation(summary = "新增广告图")
    @PostMapping("/add")
    public BaseResponse<Long> add(@RequestBody PlatformBanner banner, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (banner == null || StringUtils.isBlank(banner.getTitle()) || StringUtils.isBlank(banner.getImageUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题和图片不能为空");
        }

        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        if (banner.getIsEnabled() == null) {
            banner.setIsEnabled(1);
        }

        platformBannerMapper.insert(banner);
        adminAuditLogger.log(adminUser, "运营素材", "新增广告图", "platform_banner", banner.getId(),
                banner.getTitle(), request);
        return ResultUtils.success(banner.getId());
    }

    @Operation(summary = "更新广告图")
    @PostMapping("/update")
    public BaseResponse<Boolean> update(@RequestBody PlatformBanner banner, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (banner == null || banner.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "广告图 ID 不能为空");
        }

        boolean result = platformBannerMapper.updateById(banner) > 0;
        if (result) {
            adminAuditLogger.log(adminUser, "运营素材", "更新广告图", "platform_banner", banner.getId(),
                    banner.getTitle(), request);
        }
        return ResultUtils.success(result);
    }

    @Operation(summary = "删除广告图")
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody IdRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "广告图 ID 不能为空");
        }

        PlatformBanner oldBanner = platformBannerMapper.selectById(requestBody.getId());
        boolean result = platformBannerMapper.deleteById(requestBody.getId()) > 0;
        if (result) {
            adminAuditLogger.log(adminUser, "运营素材", "删除广告图", "platform_banner", requestBody.getId(),
                    oldBanner == null ? "" : oldBanner.getTitle(), request);
        }
        return ResultUtils.success(result);
    }

    private User getAdminLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可访问");
        }
        return loginUser;
    }

    @Data
    public static class IdRequest {
        private Long id;
    }
}
