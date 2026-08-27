package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.courseanalysis.SaveScheduleAnalysisRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.ScheduleAnalysisRecordVO;
import com.ruyi.teach.service.StudentScheduleAnalysisRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course-analysis")
@Tag(name = "课表分析")
public class CourseAnalysisController {

    @Resource
    private StudentScheduleAnalysisRecordService recordService;

    @Operation(summary = "保存课表分析结果")
    @PostMapping("/save")
    public BaseResponse<ScheduleAnalysisRecordVO> saveRecord(@RequestBody SaveScheduleAnalysisRequest request,
                                                             HttpServletRequest httpServletRequest) {
        User loginUser = getLoginUser(httpServletRequest);
        request.setUserId(loginUser.getId());
        ScheduleAnalysisRecordVO vo = recordService.saveRecord(request);
        return ResultUtils.success(vo);
    }

    @Operation(summary = "获取当前用户最近一次课表分析结果")
    @GetMapping("/latest")
    public BaseResponse<ScheduleAnalysisRecordVO> getLatestRecord(
            @RequestParam(required = false) String semesterLabel,
            HttpServletRequest httpServletRequest) {
        User loginUser = getLoginUser(httpServletRequest);
        ScheduleAnalysisRecordVO vo = recordService.getLatestRecord(loginUser.getId(), semesterLabel);
        return ResultUtils.success(vo);
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生账号可使用课表分析");
        }
        return loginUser;
    }
}
