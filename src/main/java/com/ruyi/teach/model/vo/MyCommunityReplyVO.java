package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class MyCommunityReplyVO implements Serializable {

    private Long discussionId;

    private String title;

    private Long courseId;

    private String courseName;

    /**
     * 我最近一次回复时间
     */
    private String myLastReplyTime;

    /**
     * 当前讨论总回复数
     */
    private Integer replyCount;

    /**
     * 当前讨论最后活跃时间（展示文本）
     */
    private String lastActiveTime;

    private Long lastActiveTimestamp;

    private static final long serialVersionUID = 1L;
}