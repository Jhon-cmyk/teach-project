package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.ClassAnalysisRecord;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.ClassAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analysis")
public class ClassAnalysisController {

    @Resource
    private ClassAnalysisService classAnalysisService;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Operation(summary = "提交音频和教案，开始转写任务")
    @PostMapping("/submit")
    public BaseResponse<Long> submitTask(@RequestBody SubmitAnalysisRequest request, HttpServletRequest httpRequest) {
        User currentUser = SessionUserContext.getOptional(httpRequest);
        Long teacherId = currentUser.getId();

        Long recordId = classAnalysisService.submitAnalysisTask(
                teacherId, request.getAudioUrl(), request.getPlanId(), request.getPlanText());

        classAnalysisService.asyncTranscription(recordId);

        return ResultUtils.success(recordId);
    }

    @Operation(summary = "轮询查询转写或分析的状态")
    @GetMapping("/status")
    public BaseResponse<ClassAnalysisRecord> getTaskStatus(@RequestParam("id") Long id) {
        ClassAnalysisRecord record = classAnalysisService.getById(id);
        return ResultUtils.success(record);
    }

    @Operation(summary = "生成并保存AI评课报告")
    @PostMapping("/generate-report")
    public BaseResponse<ClassAnalysisRecord> generateReport(@RequestBody GenerateReportRequest request) {
        classAnalysisService.generateAndSaveReport(request.getRecordId());
        ClassAnalysisRecord record = classAnalysisService.getById(request.getRecordId());
        return ResultUtils.success(record);
    }

    @Operation(summary = "获取当前教师的教案列表")
    @GetMapping("/my-plans")
    public BaseResponse<List<AiResource>> getMyPlans(HttpServletRequest httpRequest) {
        User currentUser = SessionUserContext.getOptional(httpRequest);
        Long teacherId = currentUser.getId();

        QueryWrapper<AiResource> wrapper = new QueryWrapper<>();
        wrapper.eq("teacher_id", teacherId)
                .eq("type", "plan")
                .orderByDesc("create_time");

        List<AiResource> plans = aiResourceMapper.selectList(wrapper);
        return ResultUtils.success(plans);
    }

    @Data
    static class SubmitAnalysisRequest {
        private String audioUrl;
        private Long planId;
        private String planText;
    }

    @Data
    static class GenerateReportRequest {
        private Long recordId;
    }

    @Operation(summary = "将评课报告保存到资源库")
    @PostMapping("/save-to-resource")
    public BaseResponse<Long> saveToResource(@RequestBody SaveToResourceRequest request, HttpServletRequest httpRequest) {
        User currentUser = SessionUserContext.getOptional(httpRequest);
        Long teacherId = currentUser.getId();

        ClassAnalysisRecord record = classAnalysisService.getById(request.getRecordId());
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评课记录不存在");
        }
        if (!"completed".equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "评课报告尚未生成完成");
        }

        AiResource resource = new AiResource();
        resource.setTeacherId(teacherId);
        resource.setType("analysis");
        resource.setTitle(record.getPlanTitleSnapshot() != null
                ? "评课报告 - " + record.getPlanTitleSnapshot()
                : "评课报告 - " + record.getId());
        resource.setContent(record.getAiReport());
        resource.setIsPublished(0);

        aiResourceMapper.insert(resource);
        return ResultUtils.success(resource.getId());
    }

    @Data
    static class SaveToResourceRequest {
        private Long recordId;
    }
}
