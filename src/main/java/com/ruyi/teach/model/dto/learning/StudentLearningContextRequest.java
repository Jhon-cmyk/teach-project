package com.ruyi.teach.model.dto.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentLearningContextRequest {

    @NotBlank(message = "所在大学不能为空")
    @Size(max = 120, message = "大学名称不能超过 120 个字符")
    private String universityName;

    @NotBlank(message = "发展目标不能为空")
    @Pattern(regexp = "postgraduate|employment|undecided", message = "发展目标不合法")
    private String developmentGoal;
}
