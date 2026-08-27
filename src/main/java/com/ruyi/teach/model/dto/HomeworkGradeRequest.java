package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HomeworkGradeRequest {

    @NotBlank(message = "试卷内容不能为空")
    @Size(max = 1_000_000, message = "试卷内容过长")
    private String paperContent;

    @NotBlank(message = "学生作答不能为空")
    @Size(max = 1_000_000, message = "学生作答内容过长")
    private String studentAnswers;
}
