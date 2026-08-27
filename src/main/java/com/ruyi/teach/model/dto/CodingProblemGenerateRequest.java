package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CodingProblemGenerateRequest {

    /**
     * 教师需求描述，例如：求两数之和的Java练习题
     */
    @NotBlank(message = "需求描述不能为空")
    @Size(max = 5000, message = "需求描述长度不能超过 5000 位")
    private String description;

    /**
     * 支持的语言列表，例如：["java", "python"]
     */
    @Size(max = 4, message = "编程语言不能超过 4 种")
    private List<
            @Pattern(
                    regexp = "^(java|python|cpp|javascript)$",
                    message = "存在不支持的编程语言"
            ) String> languages;

    /**
     * 难度：easy / medium / hard
     */
    @Pattern(regexp = "^(easy|medium|hard)?$", message = "题目难度不合法")
    private String difficulty;
}
