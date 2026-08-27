package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.CourseCategoryMapper;
import com.ruyi.teach.mapper.PlatformBannerMapper;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseCategory;
import com.ruyi.teach.model.entity.PlatformBanner;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.DashboardVO;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private static final String SCOPE_PLATFORM = "platform";

    @Resource
    private UserService userService;

    @Resource
    private CourseService courseService;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private PlatformBannerMapper platformBannerMapper;

    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @GetMapping("/metrics")
    public BaseResponse<DashboardVO> getMetrics(HttpServletRequest request) {
        getAdminLoginUser(request);

        DashboardVO dashboardVO = new DashboardVO();

        long totalUsers = userService.count();
        long totalTeachers = countUsersByRole("teacher");
        long totalBanners = platformBannerMapper.selectCount(new QueryWrapper<PlatformBanner>());
        long totalCategories = courseCategoryMapper.selectCount(new QueryWrapper<CourseCategory>());

        dashboardVO.setTotalUsers(totalUsers);
        dashboardVO.setTotalStudents(countUsersByRole("student"));
        dashboardVO.setTotalTeachers(totalTeachers);
        dashboardVO.setTotalAdmins(countUsersByRole("admin"));

        dashboardVO.setTotalCourses(courseService.count());
        dashboardVO.setTotalPlatformCourses(countCoursesBySource("platform"));
        dashboardVO.setTotalTeacherCourses(countCoursesBySource("teacher"));
        dashboardVO.setPublishedPlatformCourses(countPublishedPlatformCourses());

        dashboardVO.setTotalAiResources(countAiResources(null, null));
        dashboardVO.setPublishedAiResources(countAiResources(null, 1));
        dashboardVO.setAiPlanResources(countAiResources("plan", null));
        dashboardVO.setAiQuizResources(countAiResources("quiz", null));
        dashboardVO.setAiAnimResources(countAiResources("anim", null));

        dashboardVO.setTotalPlatformCases(countPlatformCases(null));
        dashboardVO.setPendingPlatformCases(countPlatformCases("pending"));
        dashboardVO.setApprovedPlatformCases(countPlatformCases("approved"));
        dashboardVO.setRejectedPlatformCases(countPlatformCases("rejected"));
        dashboardVO.setOfflinePlatformCases(countPlatformCases("offline"));

        dashboardVO.setTotalBanners(totalBanners);
        dashboardVO.setEnabledBanners(platformBannerMapper.selectCount(
                new QueryWrapper<PlatformBanner>().eq("is_enabled", 1)
        ));
        dashboardVO.setTotalCategories(totalCategories);
        dashboardVO.setEnabledCategories(courseCategoryMapper.selectCount(
                new QueryWrapper<CourseCategory>().eq("is_enabled", 1)
        ));
        dashboardVO.setTotalAssets(totalBanners + totalCategories);

        List<User> latestTeachers = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getUserRole, "teacher")
                .orderByDesc(User::getCreateTime)
                .last("limit 5"));

        List<DashboardVO.RecentTeacherVO> recentTeacherVOs = latestTeachers.stream().map(teacher -> {
            DashboardVO.RecentTeacherVO vo = new DashboardVO.RecentTeacherVO();
            vo.setId(teacher.getId());
            vo.setName(teacher.getUserName() != null ? teacher.getUserName() : teacher.getUserAccount());
            vo.setAccount(teacher.getUserAccount());
            vo.setUserAccount(teacher.getUserAccount());
            vo.setAvatar(teacher.getUserAvatar());
            vo.setCreateTime(formatDateTime(teacher.getCreateTime()));
            vo.setContentCount(countTeacherConfiguredContent(teacher.getId()));
            return vo;
        }).collect(Collectors.toList());

        dashboardVO.setRecentTeachers(recentTeacherVOs);

        return ResultUtils.success(dashboardVO);
    }

    private long countUsersByRole(String role) {
        return userService.count(new LambdaQueryWrapper<User>().eq(User::getUserRole, role));
    }

    private long countCoursesBySource(String sourceType) {
        return courseService.count(new QueryWrapper<Course>().eq("sourceType", sourceType));
    }

    private long countPublishedPlatformCourses() {
        return courseService.count(new QueryWrapper<Course>()
                .eq("sourceType", "platform")
                .eq("publishStatus", "published"));
    }

    private long countAiResources(String type, Integer isPublished) {
        QueryWrapper<AiResource> wrapper = new QueryWrapper<>();
        wrapper.eq("is_delete", 0);
        if (type != null) {
            wrapper.eq("type", type);
        }
        if (isPublished != null) {
            wrapper.eq("is_published", isPublished);
        }
        return aiResourceMapper.selectCount(wrapper);
    }

    private long countTeacherConfiguredContent(Long teacherId) {
        if (teacherId == null) {
            return 0;
        }
        long courseCount = courseService.count(new QueryWrapper<Course>()
                .and(wrapper -> wrapper.eq("teacherId", teacherId).or().eq("creatorId", teacherId)));
        long aiResourceCount = aiResourceMapper.selectCount(new QueryWrapper<AiResource>()
                .eq("teacher_id", teacherId)
                .eq("is_delete", 0));
        long teachingCaseCount = teachingCaseMapper.selectCount(new QueryWrapper<TeachingCase>()
                .eq("teacher_id", teacherId)
                .eq("is_delete", 0));
        return courseCount + aiResourceCount + teachingCaseCount;
    }

    private String formatDateTime(java.util.Date date) {
        if (date == null) {
            return "刚刚";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private long countPlatformCases(String status) {
        QueryWrapper<TeachingCase> wrapper = new QueryWrapper<>();
        wrapper.eq("scope", SCOPE_PLATFORM).eq("is_delete", 0);
        if (status != null) {
            wrapper.eq("status", status);
        }
        return teachingCaseMapper.selectCount(wrapper);
    }

    private User getAdminLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "admin only");
        }
        return loginUser;
    }
}
