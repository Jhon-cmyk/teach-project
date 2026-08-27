package com.ruyi.teach.model.dto;

import lombok.Data;

/**
 * 加入答疑精选请求
 */
@Data
public class CommunityFeaturedAddRequest {

    /**
     * 帖子 ID
     */
    private Long postId;

    /**
     * 回复 ID（可选）
     */
    private Long replyId;

    /**
     * 教师 ID
     */
    private Long teacherId;

    /**
     * 教师名称
     */
    private String teacherName;

    /**
     * 精选摘要
     */
    private String excerpt;

    /**
     * 是否推荐：0 / 1
     */
    private Integer isRecommended;

    /**
     * 排序值（可选）
     */
    private Integer sortOrder;
}