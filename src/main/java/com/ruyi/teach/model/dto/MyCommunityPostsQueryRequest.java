package com.ruyi.teach.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MyCommunityPostsQueryRequest implements Serializable {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 可选：discussion / homework / all
     */
    private String postType;

    private static final long serialVersionUID = 1L;
}