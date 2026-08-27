package com.ruyi.teach.model.dto;

import lombok.Data;

/**
 * 社区回复提交请求
 */
@Data
public class CommunityReplyAddRequest {

    /**
     * 帖子 ID
     */
    private Long postId;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 兼容兜底字段：当前端/Session 暂时取不到登录态时可临时传
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