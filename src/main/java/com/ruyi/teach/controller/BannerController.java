package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.mapper.PlatformBannerMapper;
import com.ruyi.teach.model.entity.PlatformBanner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/banner")
@Tag(name = "公开广告图接口")
public class BannerController {

    @Resource
    private PlatformBannerMapper platformBannerMapper;

    @Operation(summary = "获取启用中的广告图列表")
    @GetMapping("/list")
    public BaseResponse<List<PlatformBanner>> list() {
        QueryWrapper<PlatformBanner> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_enabled", 1)
                .orderByAsc("sort_order")
                .orderByDesc("create_time");
        return ResultUtils.success(platformBannerMapper.selectList(queryWrapper));
    }
}