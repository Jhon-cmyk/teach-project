package com.ruyi.teach.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CodingProblemAddRequest {

    @NotBlank(message = "题目标题不能为空")
    @Size(max = 200, message = "题目标题长度不能超过 200 位")
    private String title;

    @NotBlank(message = "题目描述不能为空")
    @Size(max = 50_000, message = "题目描述长度不能超过 50000 位")
    private String description;

    /**
     * easy/medium/hard
     */
    @Pattern(regexp = "^(easy|medium|hard)?$", message = "题目难度不合法")
    private String difficulty;

    /**
     * 支持的语言列表 ["java","python","cpp","javascript"]
     */
    @NotEmpty(message = "至少选择一种编程语言")
    @Size(max = 4, message = "编程语言不能超过 4 种")
    private List<
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

    /**
     * 所属学年学期，如 2025-2026-1
     */
    @Pattern(regexp = "^$|^\\d{4}-\\d{4}-[12]$", message = "学期格式应为 2025-2026-1")
    private String semesterLabel;

    /**
     * 多语言模板
     */
    @Valid
    @Size(max = 4, message = "代码模板不能超过 4 个")
    private List<CodingTemplateItem> templates;

    /**
     * 测试用例
     */
    @Valid
    @Size(max = 200, message = "测试用例不能超过 200 个")
    private List<CodingTestCaseItem> testCases;

    @Data
    public static class CodingTemplateItem {
        @NotBlank(message = "模板语言不能为空")
        @Size(max = 32, message = "模板语言长度不能超过 32 位")
        private String language;

        @Size(max = 100_000, message = "起始代码过长")
        private String starterCode;

        @Size(max = 100_000, message = "参考答案过长")
        private String referenceSolution;
    }

    @Data
    public static class CodingTestCaseItem {
        @Size(max = 100_000, message = "测试输入过长")
        private String input;

        @NotBlank(message = "期望输出不能为空")
        @Size(max = 100_000, message = "期望输出过长")
        private String expectedOutput;

        @jakarta.validation.constraints.Min(value = 0, message = "样例标识只能为 0 或 1")
        @Max(value = 1, message = "样例标识只能为 0 或 1")
        private Integer isSample;

        @jakarta.validation.constraints.PositiveOrZero(message = "分值不能为负数")
        @Max(value = 1000, message = "单个测试用例分值不能超过 1000")
        private Integer score;

        @jakarta.validation.constraints.PositiveOrZero(message = "排序值不能为负数")
        private Integer sortOrder;
    }
}
