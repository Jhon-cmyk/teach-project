package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.MentalStateRecord;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.MentalStateRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mental-state")
@Tag(name = "认知状态评估")
public class MentalStateController {

    @Resource
    private MentalStateRecordService mentalStateRecordService;

    /**
     * 保存一次认知状态评估结果
     */
    @Operation(summary = "保存认知状态评估结果")
    @PostMapping("/save")
    public BaseResponse<Long> save(@RequestBody MentalStateRecord record, HttpServletRequest request) {
        if (record == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = getLoginUser(request);

        // 以 session 用户为准，避免前端篡改 userId
        record.setUserId(loginUser.getId());

        // 防御性：清理不该由前端传入的字段
        record.setId(null);
        record.setIsDelete(0);
        record.setCreateTime(null);
        record.setUpdateTime(null);

        Long id = mentalStateRecordService.saveRecord(record);
        return ResultUtils.success(id);
    }

    /**
     * 查询当前登录用户的历史评估记录(用于趋势图、档案回顾等)
     */
    @Operation(summary = "获取我的评估历史")
    @GetMapping("/history")
    public BaseResponse<List<MentalStateRecord>> history(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        List<MentalStateRecord> list = mentalStateRecordService.listByUser(loginUser.getId(), limit);
        return ResultUtils.success(list);
    }

    /**
     * 登录态获取
     */
    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return loginUser;
    }
}
