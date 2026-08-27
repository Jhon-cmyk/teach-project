package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.HomeworkReminderMapper;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.dto.SendReminderRequest;
import com.ruyi.teach.model.entity.HomeworkReminder;
import com.ruyi.teach.model.entity.SysClass;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.enums.UserRole;
import com.ruyi.teach.service.RoleAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notification")
@Tag(name = "通知提醒")
public class NotificationController {

    @Resource
    private HomeworkReminderMapper homeworkReminderMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private RoleAuthorizationService roleAuthorizationService;

    // ----------------------------------------------------------------
    // 教师端：一键发送作业提醒
    // ----------------------------------------------------------------

    @Operation(summary = "教师一键发送作业提醒")
    @PostMapping("/send-homework-reminder")
    public BaseResponse<Boolean> sendHomeworkReminder(
            @Valid @RequestBody(required = false) SendReminderRequest body,
            HttpServletRequest request) {

        User loginUser = requireRole(request, UserRole.TEACHER);
        SendReminderRequest safeBody = body == null ? new SendReminderRequest() : body;

        List<SysClass> teacherClasses = sysClassMapper.selectMyClasses(loginUser.getId());
        Set<Long> teacherClassIds = teacherClasses.stream()
                .map(SysClass::getId)
                .collect(Collectors.toSet());
        if (teacherClassIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前教师没有可发送提醒的授课班级");
        }
        if (safeBody.getClassId() != null && !teacherClassIds.contains(safeBody.getClassId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能向本人授课班级发送提醒");
        }

        String msg = (safeBody.getMessage() != null && !safeBody.getMessage().isBlank())
                ? safeBody.getMessage()
                : "老师提醒您：请及时完成作业哦，加油！📚";

        Set<Long> targetClassIds = safeBody.getClassId() == null
                ? teacherClassIds
                : Set.of(safeBody.getClassId());
        boolean inserted = true;
        for (Long classId : targetClassIds) {
            HomeworkReminder reminder = new HomeworkReminder();
            reminder.setTeacherId(loginUser.getId());
            reminder.setClassId(classId);
            reminder.setMessage(msg);
            inserted = homeworkReminderMapper.insert(reminder) > 0 && inserted;
        }
        return ResultUtils.success(inserted);
    }

    // ----------------------------------------------------------------
    // 学生端：轮询检查是否有新的作业提醒
    // ----------------------------------------------------------------

    @Operation(summary = "学生端检查是否有新的作业提醒")
    @GetMapping("/check-homework-reminder")
    public BaseResponse<Map<String, Object>> checkHomeworkReminder(
            @RequestParam(defaultValue = "0") long lastCheckedAt,
            HttpServletRequest request) {
        User loginUser = requireRole(request, UserRole.STUDENT);

        Map<String, Object> result = new HashMap<>();

        Date since;
        if (lastCheckedAt > 0) {
            // 正常轮询：查上次检查时间之后的新提醒
            since = new Date(lastCheckedAt);
        } else {
            // 首次检查：往前推 10 分钟，防止学生晚几分钟开页面时漏掉提醒
            since = new Date(System.currentTimeMillis() - 10 * 60 * 1000L);
        }

        HomeworkReminder latest = homeworkReminderMapper.selectLatestForStudent(loginUser.getId(), since);

        if (latest != null) {
            result.put("hasNew", true);
            result.put("message", latest.getMessage());
            // ✅ null 安全：createTime 映射失败时用当前时间兜底，避免 NPE
            long remindAt = (latest.getCreateTime() != null)
                    ? latest.getCreateTime().getTime()
                    : System.currentTimeMillis();
            result.put("remindAt", remindAt);
        } else {
            result.put("hasNew", false);
        }
        return ResultUtils.success(result);
    }

    private User requireRole(HttpServletRequest request, UserRole role) {
        User loginUser = SessionUserContext.require(request);
        return roleAuthorizationService.requireAnyRole(loginUser, role);
    }
}
