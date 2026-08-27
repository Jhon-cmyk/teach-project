package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MicroVideoCoursePublishRequest {

    @NotNull(message = "资源 ID 不能为空")
    @Positive(message = "资源 ID 必须为正数")
    private Long resourceId;

    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    @Size(max = 100, message = "课程名称长度不能超过 100 位")
    private String courseName;

    @Size(max = 200, message = "章节标题长度不能超过 200 位")
    private String chapterTitle;

    @PositiveOrZero(message = "章节顺序不能为负数")
    private Integer sortOrder;
}
