package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.model.dto.FatigueReportRequest;
import com.ruyi.teach.model.entity.FatigueRecord;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.enums.UserRole;
import com.ruyi.teach.service.FatigueRecordService;
import com.ruyi.teach.service.RoleAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDate;

@RestController
@RequestMapping("/fatigue")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Tag(name = "疲劳检测记录")
public class FatigueRecordController {

    @Resource
    private FatigueRecordService fatigueRecordService;

    @Resource
    private RoleAuthorizationService roleAuthorizationService;

    /**
     * 前端定时上报疲劳数据（每 30 秒调一次）
     */
    @Operation(summary = "上报/更新今日疲劳数据")
    @PostMapping("/report")
    public BaseResponse<FatigueRecord> report(@Valid @RequestBody FatigueReportRequest req,
                                              HttpServletRequest request) {
        User loginUser = requireStudent(request);

        FatigueRecord record = new FatigueRecord();
        record.setCourseId(req.getCourseId());
        record.setChapterId(req.getChapterId());
        record.setYawnCount(req.getYawnCount() != null ? req.getYawnCount() : 0);
        record.setFatigueCount(req.getFatigueCount() != null ? req.getFatigueCount() : 0);
        record.setNoFaceCount(req.getNoFaceCount() != null ? req.getNoFaceCount() : 0);
        record.setNormalCount(req.getNormalCount() != null ? req.getNormalCount() : 0);
        record.setTotalDetections(req.getTotalDetections() != null ? req.getTotalDetections() : 0);
        record.setMonitorSeconds(req.getMonitorSeconds() != null ? req.getMonitorSeconds() : 0);
        record.setEvents(req.getEvents());
        record.setEarSamples(req.getEarSamples());
        record.setMarSamples(req.getMarSamples());
        record.setLastStatus(req.getLastStatus() != null ? req.getLastStatus() : "normal");

        FatigueRecord result = fatigueRecordService.saveOrUpdateToday(loginUser.getId(), record);
        return ResultUtils.success(result);
    }

    /**
     * 获取某用户今日的疲劳记录（MentalStateView 用）
     */
    @Operation(summary = "获取今日疲劳数据")
    @GetMapping("/today")
    public BaseResponse<FatigueRecord> getToday(HttpServletRequest request) {
        User loginUser = requireStudent(request);
        FatigueRecord record = fatigueRecordService.getByUserAndDate(loginUser.getId(), LocalDate.now());
        return ResultUtils.success(record);
    }

    /**
     * 获取某用户某天的疲劳记录（历史查询）
     */
    @Operation(summary = "获取指定日期的疲劳数据")
    @GetMapping("/date")
    public BaseResponse<FatigueRecord> getByDate(@RequestParam String date,
                                                 HttpServletRequest request) {
        User loginUser = requireStudent(request);
        LocalDate ld = LocalDate.parse(date);
        FatigueRecord record = fatigueRecordService.getByUserAndDate(loginUser.getId(), ld);
        return ResultUtils.success(record);
    }

    /**
     * 获取某用户最近7天的学习时长（Dashboard 折线图用）
     * 返回数组：[{date: "2026-03-22", weekday: "周六", hours: 1.5}, ...]
     */
    @Operation(summary = "获取最近7天学习时长")
    @GetMapping("/weekly")
    public BaseResponse<java.util.List<java.util.Map<String, Object>>> getWeeklyDuration(
            HttpServletRequest request) {
        User loginUser = requireStudent(request);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        String[] weekdays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            FatigueRecord record = fatigueRecordService.getByUserAndDate(loginUser.getId(), date);

            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("date", date.toString());
            int dayOfWeek = date.getDayOfWeek().getValue() % 7; // 0=Sun, 1=Mon, ...
            item.put("weekday", weekdays[dayOfWeek]);

            if (record != null && record.getMonitorSeconds() != null) {
                // 转换为小时，保留一位小数
                double hours = Math.round(record.getMonitorSeconds() / 360.0) / 10.0;
                item.put("hours", hours);
            } else {
                item.put("hours", 0);
            }
            result.add(item);
        }
        return ResultUtils.success(result);
    }

    /**
     * Heatmap data for the student dashboard.
     * Source stays consistent with /fatigue/weekly: fatigue_record.monitor_seconds.
     */
    @Operation(summary = "Get study heatmap data")
    @GetMapping("/heatmap")
    public BaseResponse<java.util.List<java.util.Map<String, Object>>> getHeatmap(
            @RequestParam(defaultValue = "180") Integer days,
            HttpServletRequest request) {
        User loginUser = requireStudent(request);
        int safeDays = days == null ? 180 : Math.max(1, Math.min(days, 366));
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);

        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            FatigueRecord record = fatigueRecordService.getByUserAndDate(loginUser.getId(), date);
            int seconds = record != null && record.getMonitorSeconds() != null
                    ? Math.max(0, record.getMonitorSeconds())
                    : 0;
            double hours = Math.round(seconds / 360.0) / 10.0;

            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("date", date.toString());
            item.put("hours", hours);
            item.put("minutes", Math.round(seconds / 60.0));
            item.put("seconds", seconds);
            result.add(item);
        }
        return ResultUtils.success(result);
    }

    private User requireStudent(HttpServletRequest request) {
        User loginUser = SessionUserContext.require(request);
        return roleAuthorizationService.requireAnyRole(loginUser, UserRole.STUDENT);
    }
}
