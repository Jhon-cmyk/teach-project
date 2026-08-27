package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.StudyHistoryMapper;
import com.ruyi.teach.model.entity.StudyHistory;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.HistoryCourseVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/history")
@Tag(name = "历史记录模块")
public class HistoryController {

    @Resource
    private StudyHistoryMapper studyHistoryMapper;

    @Operation(summary = "记录浏览历史 (防并发安全版)")
    @PostMapping("/record")
    public BaseResponse<Boolean> recordHistory(@RequestBody StudyHistory historyReq, HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);

        Long userId = loginUser.getId();
        Long courseId = historyReq.getCourseId();

        // 依靠底层的 ON DUPLICATE KEY UPDATE 解决判断与插入的原子性问题
        studyHistoryMapper.saveOrUpdateHistory(userId, courseId, new Date());

        return ResultUtils.success(true);
    }

    @Operation(summary = "获取我的历史记录")
    @GetMapping("/my")
    public BaseResponse<List<HistoryCourseVO>> listMyHistory(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);

        // 直接通过 JOIN 查询返回所需的 VO，完美保证倒序以及时间戳的正确映射
        List<HistoryCourseVO> historyList = studyHistoryMapper.selectMyHistory(loginUser.getId(), 20);

        return ResultUtils.success(historyList);
    }

    @Operation(summary = "删除我的单条学习历史")
    @DeleteMapping("/{courseId}")
    public BaseResponse<Boolean> deleteMyHistory(@PathVariable Long courseId,
                                                 HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程 ID 不合法");
        }

        LambdaQueryWrapper<StudyHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyHistory::getUserId, loginUser.getId())
                .eq(StudyHistory::getCourseId, courseId);
        studyHistoryMapper.delete(wrapper);
        return ResultUtils.success(true);
    }
}
