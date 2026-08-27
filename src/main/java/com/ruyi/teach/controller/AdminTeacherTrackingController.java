package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.TeacherCourseAssignmentMapper;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.TeacherCourseAssignment;
import com.ruyi.teach.model.entity.TeacherSchedule;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.TeacherScheduleService;
import com.ruyi.teach.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/teacher-tracking")
public class AdminTeacherTrackingController {


    @Resource
    private UserService userService;

    @Resource
    private CourseService courseService;

    @Resource
    private TeacherScheduleService teacherScheduleService;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private TeacherCourseAssignmentMapper teacherCourseAssignmentMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @GetMapping("/list")
    public BaseResponse<Page<TeacherTrackingVO>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String semester,
            HttpServletRequest request) {
        getAdminLoginUser(request);
        String targetSemester = normalizeSemester(semester);

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("userRole", "teacher");
        if (StringUtils.isNotBlank(keyword)) {
            String text = keyword.trim();
            wrapper.and(w -> w.like("userAccount", text).or().like("userName", text));
        }
        wrapper.orderByDesc("createTime");

        List<TeacherTrackingVO> all = userService.list(wrapper).stream()
                .map(teacher -> toVO(teacher, targetSemester))
                .filter(item -> StringUtils.isBlank(status)
                        || "all".equals(status)
                        || Objects.equals(status, item.getStatus()))
                .sorted(Comparator.comparing(TeacherTrackingVO::getCreateTime, Comparator.nullsLast(Date::compareTo)).reversed())
                .toList();

        long total = all.size();
        int from = (int) Math.min((current - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        Page<TeacherTrackingVO> page = new Page<>(current, size, total);
        page.setRecords(all.subList(from, to));
        return ResultUtils.success(page);
    }

    @GetMapping("/assigned-courses")
    public BaseResponse<List<AssignedCourseVO>> assignedCourses(@RequestParam Long teacherId,
                                                                @RequestParam(required = false) String semester,
                                                                HttpServletRequest request) {
        getAdminLoginUser(request);
        assertTeacher(teacherId);
        return ResultUtils.success(listAssignedCourses(teacherId, normalizeSemester(semester)));
    }

    @PostMapping("/assign-courses")
    public BaseResponse<Boolean> assignCourses(@RequestBody AssignCoursesRequest requestBody,
                                               HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        if (requestBody == null || requestBody.getTeacherId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师不能为空");
        }
        User teacher = assertTeacher(requestBody.getTeacherId());
        String semester = normalizeSemester(requestBody.getSemester());
        List<Long> courseIds = requestBody.getCourseIds() == null ? List.of() : requestBody.getCourseIds();
        LinkedHashSet<Long> uniqueCourseIds = courseIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Course> courses = uniqueCourseIds.isEmpty() ? List.of() : courseService.listByIds(uniqueCourseIds);
        if (courses.size() != uniqueCourseIds.size()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在无效课程");
        }

        QueryWrapper<TeacherCourseAssignment> removeWrapper = new QueryWrapper<>();
        removeWrapper.eq("teacher_id", requestBody.getTeacherId())
                .eq("semester", semester)
                .eq("is_delete", 0);
        teacherCourseAssignmentMapper.delete(removeWrapper);

        Date now = new Date();
        for (Long courseId : uniqueCourseIds) {
            TeacherCourseAssignment assignment = new TeacherCourseAssignment();
            assignment.setTeacherId(requestBody.getTeacherId());
            assignment.setCourseId(courseId);
            assignment.setSemester(semester);
            assignment.setAssignedBy(admin.getId());
            assignment.setCreateTime(now);
            assignment.setUpdateTime(now);
            assignment.setIsDelete(0);
            teacherCourseAssignmentMapper.insert(assignment);
        }

        String courseNames = courses.stream().map(Course::getName).collect(Collectors.joining("、"));
        adminAuditLogger.log(admin, "教师配置跟踪", "设置本学期授课课程", "teacher_course_assignment", teacher.getId(),
                teacher.getUserAccount() + " / " + semester + " / " + StringUtils.defaultIfBlank(courseNames, "清空课程"), request);
        return ResultUtils.success(true);
    }

    private TeacherTrackingVO toVO(User teacher, String semester) {
        long courseCount = countCourses(teacher.getId());
        long aiCount = aiResourceMapper.selectCount(new QueryWrapper<AiResource>()
                .eq("teacher_id", teacher.getId())
                .eq("is_delete", 0));
        long caseCount = teachingCaseMapper.selectCount(new QueryWrapper<TeachingCase>()
                .eq("teacher_id", teacher.getId())
                .eq("is_delete", 0));

        TeacherTrackingVO vo = new TeacherTrackingVO();
        vo.setId(teacher.getId());
        vo.setAccount(teacher.getUserAccount());
        vo.setName(StringUtils.defaultIfBlank(teacher.getUserName(), teacher.getUserAccount()));
        vo.setCreateTime(teacher.getCreateTime());
        vo.setCourseCount(courseCount);
        vo.setAiResourceCount(aiCount);
        vo.setCaseCount(caseCount);
        vo.setTotalContentCount(courseCount + aiCount + caseCount);
        vo.setSemester(semester);
        List<AssignedCourseVO> assignedCourses = listAssignedCourses(teacher.getId(), semester);
        vo.setAssignedCourses(assignedCourses);
        vo.setAssignedCourseCount((long) assignedCourses.size());
        vo.setStatus(vo.getTotalContentCount() > 0 || !assignedCourses.isEmpty() ? "configured" : "not_configured");
        vo.setLastContentTime(resolveLastContentTime(teacher.getId()));
        return vo;
    }

    private List<AssignedCourseVO> listAssignedCourses(Long teacherId, String semester) {
        List<TeacherSchedule> schedules = teacherScheduleService.list(new QueryWrapper<TeacherSchedule>()
                .eq("teacher_id", teacherId)
                .eq("semester_label", semester)
                .eq("is_delete", 0)
                .orderByAsc("day_of_week", "start_period"));
        if (schedules.isEmpty()) {
            return List.of();
        }
        List<AssignedCourseVO> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (TeacherSchedule schedule : schedules) {
            if (StringUtils.isBlank(schedule.getCourseName()) || !seen.add(schedule.getCourseName())) {
                continue;
            }
            AssignedCourseVO vo = new AssignedCourseVO();
            vo.setId(schedule.getId());
            vo.setName(schedule.getCourseName());
            vo.setSourceType("schedule");
            vo.setPublishStatus(schedule.getClassName());
            result.add(vo);
        }
        return result;
    }

    private long countCourses(Long teacherId) {
        return teacherScheduleService.count(new QueryWrapper<TeacherSchedule>()
                .eq("teacher_id", teacherId)
                .eq("is_delete", 0));
    }

    private Date resolveLastContentTime(Long teacherId) {
        Date latest = null;
        Course course = courseService.getOne(new QueryWrapper<Course>()
                .and(w -> w.eq("teacherId", teacherId).or().eq("creatorId", teacherId))
                .orderByDesc("updateTime")
                .last("limit 1"), false);
        if (course != null) {
            latest = newer(latest, course.getUpdateTime());
        }
        AiResource ai = aiResourceMapper.selectOne(new QueryWrapper<AiResource>()
                .eq("teacher_id", teacherId)
                .eq("is_delete", 0)
                .orderByDesc("update_time")
                .last("limit 1"));
        if (ai != null) {
            latest = newer(latest, ai.getUpdateTime());
        }
        TeachingCase teachingCase = teachingCaseMapper.selectOne(new QueryWrapper<TeachingCase>()
                .eq("teacher_id", teacherId)
                .eq("is_delete", 0)
                .orderByDesc("update_time")
                .last("limit 1"));
        if (teachingCase != null) {
            latest = newer(latest, teachingCase.getUpdateTime());
        }
        return latest;
    }

    private Date newer(Date current, Date candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.after(current)) {
            return candidate;
        }
        return current;
    }

    private User getAdminLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可访问");
        }
        return loginUser;
    }

    private User assertTeacher(Long teacherId) {
        User teacher = userService.getById(teacherId);
        if (teacher == null || !"teacher".equals(teacher.getUserRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师不存在");
        }
        return teacher;
    }

    private String normalizeSemester(String semester) {
        if (StringUtils.isNotBlank(semester)) {
            return semester.trim();
        }
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        if (month >= 9) {
            return year + "-" + (year + 1) + "-1";
        }
        return (year - 1) + "-" + year + "-2";
    }

    @Data
    public static class TeacherTrackingVO {
        private Long id;
        private String account;
        private String name;
        private Date createTime;
        private String semester;
        private Long courseCount;
        private Long assignedCourseCount;
        private Long aiResourceCount;
        private Long caseCount;
        private Long totalContentCount;
        private String status;
        private Date lastContentTime;
        private List<AssignedCourseVO> assignedCourses;
    }

    @Data
    public static class AssignedCourseVO {
        private Long id;
        private String name;
        private String sourceType;
        private String publishStatus;
    }

    @Data
    public static class AssignCoursesRequest {
        private Long teacherId;
        private String semester;
        private List<Long> courseIds;
    }
}
