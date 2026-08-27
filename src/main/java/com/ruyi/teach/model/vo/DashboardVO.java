package com.ruyi.teach.model.vo;

import lombok.Data;
import java.util.List;

@Data
public class DashboardVO {
    // 全站总用户数
    private Long totalUsers;
    private Long totalStudents;
    // 入驻教师总数
    private Long totalTeachers;
    private Long totalAdmins;
    // 全站课程总数
    private Long totalCourses;
    private Long totalPlatformCourses;
    private Long totalTeacherCourses;
    private Long publishedPlatformCourses;
    private Long totalAiResources;
    private Long publishedAiResources;
    private Long aiPlanResources;
    private Long aiQuizResources;
    private Long aiAnimResources;
    private Long totalPlatformCases;
    private Long pendingPlatformCases;
    private Long approvedPlatformCases;
    private Long rejectedPlatformCases;
    private Long offlinePlatformCases;
    private Long totalBanners;
    private Long enabledBanners;
    private Long totalCategories;
    private Long enabledCategories;
    private Long totalAssets;
    // 最新入驻的教师列表（用于右侧列表展示）
    private List<RecentTeacherVO> recentTeachers;

    @Data
    public static class RecentTeacherVO {
        private Long id;
        private String name;
        private String account;
        private String userAccount;
        private String avatar;
        private String createTime;
        private Long contentCount;
    }
}
