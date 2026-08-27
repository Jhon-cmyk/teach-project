package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CodingProblemVO {

    private Long id;

    private String title;

    private String description;

    private String difficulty;

    private List<String> languages;

    private Integer timeLimitMs;

    private Integer memoryLimitKb;

    private Long courseId;

    private String semesterLabel;

    private Integer isPublic;

    private Date createTime;

    /**
     * 样例用例（学生可见）
     */
    private List<CodingTestCaseVO> sampleTestCases;

    /**
     * 当前用户的各语言模板
     */
    private List<CodingTemplateVO> templates;

    /**
     * 教师视角下可见的全部用例（学生列表接口返回 null）
     */
    private List<CodingTestCaseVO> allTestCases;

    /**
     * 教师视角下可见的参考解（学生列表接口返回 null）
     */
    private List<CodingTemplateVO> templatesWithSolution;

    /**
     * 已发布的班级数量（教师列表）
     */
    private Integer publishCount;

    /**
     * 提交人数（教师列表，去重按学生统计）
     */
    private Integer submissionCount;

    /**
     * 当前学生对该题的最佳得分；未尝试为 null（学生列表）
     */
    private Integer myBestScore;

    /**
     * 当前学生的提交次数（学生列表）
     */
    private Integer myAttemptCount;

    private Date deadline;

    private List<String> publishedClasses;

    @Data
    public static class CodingTestCaseVO {
        private Long id;
        private String input;
        private String expectedOutput;
        private Integer isSample;
        private Integer score;
        private Integer sortOrder;
    }

    @Data
    public static class CodingTemplateVO {
        private String language;
        private String starterCode;
        private String referenceSolution;
    }
}
