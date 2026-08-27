package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonalPracticeCreateRequest {
    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    @Positive(message = "章节 ID 必须为正数")
    private Long chapterId;

    @NotBlank(message = "知识点不能为空")
    @Size(max = 200, message = "知识点长度不能超过 200 位")
    private String knowledgeName;
}
