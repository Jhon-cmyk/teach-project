package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.UserLoginLogMapper;
import com.ruyi.teach.mapper.UserMapper;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.FatigueRecord;
import com.ruyi.teach.model.entity.SysClass;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.UserLoginLog;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.FatigueRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/teacher/dashboard")
@Tag(name = "教师端-数据看板")
public class TeacherDashboardController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private UserLoginLogMapper userLoginLogMapper;

    @Resource
    private CourseService courseService;

    @Resource
    private FatigueRecordService fatigueRecordService;

    @Operation(summary = "获取工作台核心数据")
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> getDashboardStats(
            HttpServletRequest request,
            @RequestParam(value = "activeRange", required = false, defaultValue = "semester") String activeRange
    ) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || !"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅限教师访问");
        }
        Long teacherId = loginUser.getId();

        Map<String, Object> stats = new HashMap<>();

        long courseCount = courseService.count(
                new QueryWrapper<Course>().eq("teacherId", teacherId).eq("isDelete", 0)
        );

        long studentCount = userMapper.countStudentsByTeacherId(teacherId);
        List<SysClass> teacherClasses = sysClassMapper.selectMyClasses(teacherId);
        int classCount = teacherClasses == null ? 0 : teacherClasses.size();

        long totalMonitorSeconds = 0L;
        LocalDate today = LocalDate.now();

        List<Long> studentIds = userMapper.listStudentIdsByTeacherId(teacherId);
        if (studentIds != null && !studentIds.isEmpty()) {
            for (Long studentId : studentIds) {
                FatigueRecord record = fatigueRecordService.getByUserAndDate(studentId, today);
                if (record != null && record.getMonitorSeconds() != null) {
                    totalMonitorSeconds += record.getMonitorSeconds();
                }
            }
        }

        System.out.println("当前老师ID = " + teacherId);
        System.out.println("当前老师名下学生IDs = " + studentIds);

        long avgStudyTime = 0L;
        if (studentCount > 0) {
            avgStudyTime = Math.round(totalMonitorSeconds / 60.0 / studentCount);
        }

        ActiveRangeTime activeRangeTime = resolveActiveRangeTime(activeRange, today);
        List<Map<String, Object>> activeTimeDistribution = buildDefaultActiveTimeDistribution();
        if (studentIds != null && !studentIds.isEmpty()) {
            List<Map<String, Object>> loginLogMaps = userLoginLogMapper.selectMaps(
                    new QueryWrapper<UserLoginLog>()
                            .select("user_id", "login_time")
                            .in("user_id", studentIds)
                            .ge("login_time", Timestamp.valueOf(activeRangeTime.startTime()))
                            .lt("login_time", Timestamp.valueOf(activeRangeTime.endTime()))
            );

            activeTimeDistribution = buildActiveTimeDistributionFromLogMaps(loginLogMaps);
        }

        stats.put("totalStudents", studentCount);
        stats.put("totalCourses", courseCount);
        stats.put("classCount", classCount);
        stats.put("aiUsage", 12);
        stats.put("avgStudyTime", avgStudyTime);
        stats.put("activeTimeDistribution", activeTimeDistribution);
        stats.put("activeRange", activeRangeTime.key());
        stats.put("activeRangeLabel", activeRangeTime.label());

        long completed = (long) (studentCount * 0.6);
        long doing = (long) (studentCount * 0.3);
        long unstarted = studentCount - completed - doing;

        List<Map<String, Object>> pieData = new ArrayList<>();
        pieData.add(createPieNode("已完成", completed));
        pieData.add(createPieNode("进行中", doing));
        pieData.add(createPieNode("未开始", unstarted));

        stats.put("pieChartData", pieData);

        return ResultUtils.success(stats);
    }

    private ActiveRangeTime resolveActiveRangeTime(String activeRange, LocalDate today) {
        String normalizedRange = activeRange == null ? "semester" : activeRange.trim().toLowerCase();
        LocalDate startDate;
        String key;
        String label;

        switch (normalizedRange) {
            case "7d":
            case "7days":
            case "week":
                startDate = today.minusDays(6);
                key = "7d";
                label = "近7天";
                break;
            case "30d":
            case "30days":
            case "month":
                startDate = today.minusDays(29);
                key = "30d";
                label = "近30天";
                break;
            case "semester":
            case "term":
                startDate = resolveCurrentSemesterStart(today);
                key = "semester";
                label = "本学期";
                break;
            case "today":
            default:
                startDate = today;
                key = "today";
                label = "今日";
                break;
        }

        return new ActiveRangeTime(key, label, startDate.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    private LocalDate resolveCurrentSemesterStart(LocalDate today) {
        int month = today.getMonthValue();
        if (month >= 9) {
            return LocalDate.of(today.getYear(), 9, 1);
        }
        if (month >= 2) {
            return LocalDate.of(today.getYear(), 2, 1);
        }
        return LocalDate.of(today.getYear() - 1, 9, 1);
    }

    private record ActiveRangeTime(String key, String label, LocalDateTime startTime, LocalDateTime endTime) {
    }

    private List<Map<String, Object>> buildDefaultActiveTimeDistribution() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(createActiveNode("8-10点", 0L));
        list.add(createActiveNode("10-12点", 0L));
        list.add(createActiveNode("14-16点", 0L));
        list.add(createActiveNode("19-21点", 0L));
        list.add(createActiveNode("21-23点", 0L));
        return list;
    }

    private List<Map<String, Object>> buildActiveTimeDistributionFromLogMaps(List<Map<String, Object>> loginLogMaps) {
        LinkedHashMap<String, Set<Long>> bucketUserMap = new LinkedHashMap<>();
        bucketUserMap.put("8-10点", new HashSet<>());
        bucketUserMap.put("10-12点", new HashSet<>());
        bucketUserMap.put("14-16点", new HashSet<>());
        bucketUserMap.put("19-21点", new HashSet<>());
        bucketUserMap.put("21-23点", new HashSet<>());

        System.out.println("loginLogMaps = " + loginLogMaps);

        if (loginLogMaps != null) {
            for (Map<String, Object> row : loginLogMaps) {
                if (row == null) {
                    continue;
                }

                Long userId = toLong(
                        row.containsKey("userId") ? row.get("userId") : row.get("user_id")
                );

                java.util.Date loginTime = toDate(
                        row.containsKey("loginTime") ? row.get("loginTime") : row.get("login_time")
                );

                System.out.println("row = " + row + ", parsed userId = " + userId + ", parsed loginTime = " + loginTime);

                if (userId == null || loginTime == null) {
                    continue;
                }

                String timeRange = resolveTimeRange(loginTime);
                if (timeRange != null && bucketUserMap.containsKey(timeRange)) {
                    bucketUserMap.get(timeRange).add(userId);
                }
            }
        }

        System.out.println("登录日志查询结果 = " + loginLogMaps);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Set<Long>> entry : bucketUserMap.entrySet()) {
            result.add(createActiveNode(entry.getKey(), (long) entry.getValue().size()));
        }

        System.out.println("activeTimeDistribution result = " + result);
        return result;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private java.util.Date toDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof java.util.Date) {
            return (java.util.Date) value;
        }

        if (value instanceof java.sql.Timestamp) {
            return new java.util.Date(((java.sql.Timestamp) value).getTime());
        }

        if (value instanceof java.time.LocalDateTime) {
            return java.sql.Timestamp.valueOf((java.time.LocalDateTime) value);
        }

        if (value instanceof java.time.LocalDate) {
            return java.sql.Timestamp.valueOf(((java.time.LocalDate) value).atStartOfDay());
        }

        if (value instanceof String) {
            String text = ((String) value).trim();
            try {
                return java.sql.Timestamp.valueOf(text.replace("T", " "));
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String resolveTimeRange(java.util.Date loginTime) {
        if (loginTime == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(loginTime);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 8 && hour < 10) {
            return "8-10点";
        }
        if (hour >= 10 && hour < 12) {
            return "10-12点";
        }
        if (hour >= 14 && hour < 16) {
            return "14-16点";
        }
        if (hour >= 19 && hour < 21) {
            return "19-21点";
        }
        if (hour >= 21 && hour < 23) {
            return "21-23点";
        }
        return null;
    }

    private Map<String, Object> createActiveNode(String timeRange, Long count) {
        Map<String, Object> map = new HashMap<>();
        map.put("timeRange", timeRange);
        map.put("count", count);
        return map;
    }

    private Map<String, Object> createPieNode(String name, Long value) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }
}
