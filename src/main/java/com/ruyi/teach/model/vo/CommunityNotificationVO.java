package com.ruyi.teach.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CommunityNotificationVO implements Serializable {

    private Long id;

    private String type;

    private Long postId;

    private Long replyId;

    private String title;

    private String content;

    @JsonProperty("isRead")
    private Boolean isRead;

    private String createdAt;

    private static final long serialVersionUID = 1L;
}