package com.ruyi.teach.model.dto;

import lombok.Data;

/**
 * 作业互助 - 发布提问请求
 */
@Data
public class HomeworkHelpAddRequest {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 课程 ID
     */
    private Long courseId;

    /**
     * 课程名称（前端可直接传，避免后端本轮额外查课程表）
     */
    private String courseName;

    /**
     * 兼容兜底字段：当前端/Session 暂时取不到登录态时可临时传
     * 后续接入统一登录态后可删除
     */
    private Long userId;

    /**
     * 兼容兜底字段
     */
    private String authorName;

    /**
     * 兼容兜底字段：0 学生 / 1 教师
     */
    private Integer isTeacher;
}