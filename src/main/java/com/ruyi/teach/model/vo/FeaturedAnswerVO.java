package com.ruyi.teach.model.vo;

import lombok.Data;

/**
 * 答疑精选列表项 VO
 * 对齐前端 FeaturedAnswerItem
 */
@Data
public class FeaturedAnswerVO {

    private Long id;

    /** 对应讨论帖子 ID（前端跳转详情用） */
    private Long discussionId;

    private String title;

    private Long courseId;

    private String courseName;

    /** 精选回答摘要 */
    private String excerpt;

    /** 答疑教师名称 */
    private String teacherName;

    /** 更新时间（格式化文本） */
    private String updatedAt;

    private boolean isRecommended;
}
