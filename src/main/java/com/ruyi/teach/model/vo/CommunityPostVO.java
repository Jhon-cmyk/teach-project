package com.ruyi.teach.model.vo;

import lombok.Data;

/**
 * 讨论/作业互助列表项 VO
 * 对齐前端 DiscussionItem 和 HomeworkQuestionItem
 */
@Data
public class CommunityPostVO {

    private Long id;

    private String title;

    private Long courseId;

    private String courseName;

    private int replyCount;

    private int viewCount;

    /** 最后活跃时间（格式化文本，如 "10 分钟前"） */
    private String lastActiveTime;

    /** 最后活跃时间戳（毫秒，用于前端排序） */
    private Long lastActiveTimestamp;

    private Boolean isHot;
    private Boolean isTeacherAnswered;
    // ---- 以下字段仅作业互助使用 ----

    /** 问题状态: open / resolved */
    private String status;

    /** 摘要（作业互助页用） */
    private String excerpt;

    /** 提问人 */
    private String authorName;
}
