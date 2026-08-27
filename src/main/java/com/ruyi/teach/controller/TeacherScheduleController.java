package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.MajorCurriculumCourseMapper;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.dto.TeacherScheduleAddRequest;
import com.ruyi.teach.model.dto.TeacherScheduleUpdateRequest;
import com.ruyi.teach.model.entity.MajorCurriculumCourse;
import com.ruyi.teach.model.entity.SysClass;
import com.ruyi.teach.model.entity.TeacherSchedule;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.TeacherScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teacher/schedule")
@Tag(name = "教师排课管理")
public class TeacherScheduleController {

    @Resource
    private TeacherScheduleService teacherScheduleService;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private MajorCurriculumCourseMapper majorCurriculumCourseMapper;

    @Operation(summary = "查询当前教师的排课列表")
    @GetMapping("/list")
    public BaseResponse<List<TeacherSchedule>> listSchedules(
            @RequestParam(required = false) String semesterLabel,
            HttpServletRequest request) {

        User loginUser = getLoginUser(request);

        QueryWrapper<TeacherSchedule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teacher_id", loginUser.getId());

        if (StringUtils.isNotBlank(semesterLabel)) {
            queryWrapper.eq("semester_label", semesterLabel.trim());
        }

        queryWrapper.orderByAsc("day_of_week", "start_period");
        List<TeacherSchedule> list = teacherScheduleService.list(queryWrapper);
        return ResultUtils.success(list);
    }

    @Operation(summary = "查询当前学生所在班级课表")
    @GetMapping("/student-list")
    public BaseResponse<List<TeacherSchedule>> listStudentSchedules(
            @RequestParam(required = false) String semesterLabel,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可查看班级课表");
        }
        if (loginUser.getClassId() == null) {
            return ResultUtils.success(List.of());
        }
        SysClass sysClass = sysClassMapper.selectById(loginUser.getClassId());
        if (sysClass == null || StringUtils.isBlank(sysClass.getName())) {
            return ResultUtils.success(List.of());
        }

        QueryWrapper<TeacherSchedule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_name", sysClass.getName());
        if (StringUtils.isNotBlank(semesterLabel)) {
            queryWrapper.eq("semester_label", semesterLabel.trim());
        }
        queryWrapper.orderByAsc("day_of_week", "start_period");
        return ResultUtils.success(teacherScheduleService.list(queryWrapper));
    }

    @Operation(summary = "按班级和学期查询专业培养课程")
    @GetMapping("/curriculum-options")
    public BaseResponse<List<MajorCurriculumCourse>> curriculumOptions(
            @RequestParam String className,
            @RequestParam(required = false) Integer semesterNo,
            HttpServletRequest request) {
        getLoginUser(request);
        if (StringUtils.isBlank(className)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "班级不能为空");
        }
        SysClass sysClass = sysClassMapper.selectOne(new QueryWrapper<SysClass>()
                .eq("name", className.trim())
                .last("limit 1"));
        if (sysClass == null || StringUtils.isBlank(sysClass.getMajor())) {
            return ResultUtils.success(List.of());
        }
        QueryWrapper<MajorCurriculumCourse> wrapper = new QueryWrapper<>();
        wrapper.eq("major", sysClass.getMajor())
                .eq("is_delete", 0);
        if (semesterNo != null) {
            wrapper.eq("semester_no", semesterNo);
        }
        wrapper.orderByAsc("semester_no").orderByAsc("sort_order").orderByAsc("id");
        return ResultUtils.success(majorCurriculumCourseMapper.selectList(wrapper));
    }

    @Operation(summary = "新增排课")
    @PostMapping("/add")
    public BaseResponse<Long> addSchedule(@RequestBody TeacherScheduleAddRequest addRequest, HttpServletRequest request) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = getLoginUser(request);

        TeacherSchedule schedule = new TeacherSchedule();
        BeanUtils.copyProperties(addRequest, schedule);
        schedule.setTeacherId(loginUser.getId());

        boolean result = teacherScheduleService.save(schedule);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(schedule.getId());
    }

    @Operation(summary = "修改排课")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSchedule(@RequestBody TeacherScheduleUpdateRequest updateRequest, HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = getLoginUser(request);

        TeacherSchedule oldSchedule = teacherScheduleService.getById(updateRequest.getId());
        if (oldSchedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "排课记录不存在");
        }
        if (!oldSchedule.getTeacherId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权修改他人排课");
        }

        TeacherSchedule schedule = new TeacherSchedule();
        BeanUtils.copyProperties(updateRequest, schedule);

        boolean result = teacherScheduleService.updateById(schedule);
        return ResultUtils.success(result);
    }

    @Operation(summary = "删除排课")
    @PostMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteSchedule(@PathVariable Long id, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = getLoginUser(request);

        TeacherSchedule oldSchedule = teacherScheduleService.getById(id);
        if (oldSchedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "排课记录不存在");
        }
        if (!oldSchedule.getTeacherId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除他人排课");
        }

        boolean result = teacherScheduleService.removeById(id);
        return ResultUtils.success(result);
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return loginUser;
    }
}
