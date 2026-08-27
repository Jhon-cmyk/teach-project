package com.ruyi.teach.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommunityNotificationQueryRequest implements Serializable {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     * 0 / 1
     */
    private Integer isRead;

    /**
     * post_replied / post_resolved / post_featured / followed_discussion_updated
     */
    private String type;

    private static final long serialVersionUID = 1L;
}