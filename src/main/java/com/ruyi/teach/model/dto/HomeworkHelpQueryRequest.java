package com.ruyi.teach.model.dto;

import com.ruyi.teach.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 作业互助列表查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HomeworkHelpQueryRequest extends PageRequest {

    /** 课程ID筛选 */
    private Long courseId;

    /** 状态筛选: all / open / resolved / teacher */
    private String status;

    /** 关键词搜索 */
    private String keyword;
}
