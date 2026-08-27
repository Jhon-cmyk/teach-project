package com.ruyi.teach.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MyCommunityRepliesQueryRequest implements Serializable {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    private static final long serialVersionUID = 1L;
}