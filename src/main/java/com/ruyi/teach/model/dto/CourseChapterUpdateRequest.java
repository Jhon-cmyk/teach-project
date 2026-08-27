package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseChapterUpdateRequest {

    @NotNull(message = "章节 ID 不能为空")
    @Positive(message = "章节 ID 必须为正数")
    private Long id;

    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    @Pattern(regexp = ".*\\S.*", message = "章节标题不能为空")
    @Size(max = 200, message = "章节标题长度不能超过 200 位")
    private String title;

    @Size(max = 2048, message = "视频地址长度不能超过 2048 位")
    private String videoUrl;

    @PositiveOrZero(message = "章节顺序不能为负数")
    private Integer sortOrder;

    @Size(max = 1_000_000, message = "交互课件内容过长")
    private String animHtml;
}
