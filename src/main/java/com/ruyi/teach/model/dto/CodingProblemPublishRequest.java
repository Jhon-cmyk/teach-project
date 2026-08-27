package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CodingProblemPublishRequest {

    @NotNull(message = "题目 ID 不能为空")
    @Positive(message = "题目 ID 必须为正数")
    private Long problemId;

    @NotEmpty(message = "至少选择一个班级")
    @Size(max = 200, message = "一次最多发布到 200 个班级")
    private List<@Positive(message = "班级 ID 必须为正数") Long> classIds;

    @Positive(message = "章节 ID 必须为正数")
    private Long chapterId;

    @Future(message = "截止时间必须晚于当前时间")
    private Date deadline;
}
