package com.ruyi.teach.model.dto;

import com.ruyi.teach.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 答疑精选列表查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FeaturedAnswersQueryRequest extends PageRequest {

    /** 课程ID筛选 */
    private Long courseId;

    /** 排序方式: latest / recommended */
    private String sort;

    /** 关键词搜索 */
    private String keyword;
}
