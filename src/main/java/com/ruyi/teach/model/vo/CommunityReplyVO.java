package com.ruyi.teach.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 回复 VO
 * 对齐前端 DiscussionReply
 */
@Data
public class CommunityReplyVO {

    private Long id;

    private Long userId;

    private String authorName;

    private String content;

    /** 发布时间（格式化文本） */
    private String createdAt;

    @JsonProperty("isTeacher")
    private boolean isTeacher;
}
