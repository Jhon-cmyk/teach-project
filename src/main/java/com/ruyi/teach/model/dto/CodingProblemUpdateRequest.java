package com.ruyi.teach.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CodingProblemUpdateRequest {

    @NotNull(message = "题目 ID 不能为空")
    @Positive(message = "题目 ID 必须为正数")
    private Long id;

    @NotBlank(message = "题目标题不能为空")
    @Size(max = 200, message = "题目标题长度不能超过 200 位")
    private String title;

    @NotBlank(message = "题目描述不能为空")
    @Size(max = 50_000, message = "题目描述长度不能超过 50000 位")
    private String description;

    @Pattern(regexp = "^(easy|medium|hard)?$", message = "题目难度不合法")
    private String difficulty;

    @Size(min = 1, max = 4, message = "编程语言必须在 1 到 4 种之间")
    private java.util.List<
            @Pattern(
                    regexp = "^(java|python|cpp|javascript)$",
                    message = "存在不支持的编程语言"
            ) String> languages;

    @Positive(message = "时间限制必须为正数")
    @Max(value = 60_000, message = "时间限制不能超过 60000 毫秒")
    private Integer timeLimitMs;

    @Positive(message = "内存限制必须为正数")
    @Max(value = 1_048_576, message = "内存限制不能超过 1048576 KB")
    private Integer memoryLimitKb;

    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    @Pattern(regexp = "^$|^\\d{4}-\\d{4}-[12]$", message = "学期格式应为 2025-2026-1")
    private String semesterLabel;

    @Valid
    @Size(max = 4, message = "代码模板不能超过 4 个")
    private java.util.List<CodingProblemAddRequest.CodingTemplateItem> templates;

    @Valid
    @Size(max = 200, message = "测试用例不能超过 200 个")
    private java.util.List<CodingProblemAddRequest.CodingTestCaseItem> testCases;
}
