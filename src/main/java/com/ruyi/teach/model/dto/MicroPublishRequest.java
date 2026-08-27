package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MicroPublishRequest {

    @NotNull(message = "渲染任务 ID 不能为空")
    @Positive(message = "渲染任务 ID 必须为正数")
    private Long taskId;

    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    @Size(max = 100, message = "课程名称长度不能超过 100 位")
    private String courseName;

    @Size(max = 200, message = "资源标题长度不能超过 200 位")
    private String title;

    @Size(max = 200, message = "章节标题长度不能超过 200 位")
    private String chapterTitle;

    @PositiveOrZero(message = "章节顺序不能为负数")
    private Integer sortOrder;

    @Size(max = 32, message = "时长描述长度不能超过 32 位")
    private String duration;

    @Size(max = 32, message = "发布方式长度不能超过 32 位")
    private String publishMode;

    @Size(max = 200, message = "班级数量不能超过 200 个")
    private List<@Positive(message = "班级 ID 必须为正数") Long> classIds;
}
