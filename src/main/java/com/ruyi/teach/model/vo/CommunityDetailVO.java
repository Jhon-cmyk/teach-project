package com.ruyi.teach.model.vo;

import lombok.Data;
import java.util.List;

/**
 * 讨论详情 VO
 * 对齐前端 DiscussionDetail
 */
@Data
public class CommunityDetailVO {

    private Long id;

    private String title;

    private String content;

    private Long courseId;

    private String courseName;

    private String authorName;

    /** 发布时间（格式化文本） */
    private String createdAt;

    private int replyCount;

    private int viewCount;

    /** 最后活跃时间（格式化文本） */
    private String lastActiveTime;

    private Boolean isHot;

    private Boolean isTeacherAnswered;

    /**
     * very small 补充：
     * homework / discussion
     */
    private String postType;

    /**
     * very small 补充：
     * open / resolved
     */
    private String status;

    /** 回复列表 */
    private List<CommunityReplyVO> replies;
}