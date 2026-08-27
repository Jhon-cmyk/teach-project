package com.ruyi.teach.model.dto;

import com.ruyi.teach.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学习交流讨论列表查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommunityQueryRequest extends PageRequest {

    /** 课程ID筛选 */
    private Long courseId;

    /** 排序方式: latest / hot */
    private String sort;

    /** 关键词搜索 */
    private String keyword;
}
