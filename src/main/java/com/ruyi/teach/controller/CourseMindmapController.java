package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.course.CourseMindmapRegenerateRequest;
import com.ruyi.teach.model.vo.CourseMindmapVO;
import com.ruyi.teach.service.CourseMindmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course")
@Tag(name = "课程思维导图")
public class CourseMindmapController {

    @Resource
    private CourseMindmapService courseMindmapService;

    @Operation(summary = "获取课程总思维导图")
    @GetMapping("/mindmap")
    public BaseResponse<CourseMindmapVO> getCourseMindmap(@RequestParam("courseId") Long courseId) {
        return ResultUtils.success(courseMindmapService.getCourseMindmap(courseId));
    }

    @Operation(summary = "重新生成课程总思维导图")
    @PostMapping("/mindmap/regenerate")
    public BaseResponse<CourseMindmapVO> regenerateCourseMindmap(
            @RequestBody CourseMindmapRegenerateRequest request
    ) {
        if (request == null || request.getCourseId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "courseId 不能为空");
        }
        return ResultUtils.success(courseMindmapService.regenerateCourseMindmap(request.getCourseId()));
    }
}