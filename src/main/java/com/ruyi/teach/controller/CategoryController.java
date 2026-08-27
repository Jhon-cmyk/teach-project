package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.mapper.CourseCategoryMapper;
import com.ruyi.teach.model.entity.CourseCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
@Tag(name = "公开课程分类接口")
public class CategoryController {

    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Operation(summary = "获取启用中的分类图标列表")
    @GetMapping("/list")
    public BaseResponse<List<CourseCategory>> list() {
        QueryWrapper<CourseCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_enabled", 1)
                .orderByAsc("sort_order")
                .orderByDesc("create_time")
                .last("LIMIT 18");
        return ResultUtils.success(courseCategoryMapper.selectList(queryWrapper));
    }
}
