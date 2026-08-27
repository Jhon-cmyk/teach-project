package com.ruyi.teach.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CodingRunResultVO {

    private String language;

    private String status;

    private Boolean accepted;

    private String stdout;

    private String stderr;

    private String compileOutput;

    private String statusDescription;

    private Integer exitCode;

    private String time;

    private Integer memory;

    /**
     * 提交模式下才有以下字段
     */
    private Long submissionId;

    private Integer passedCount;

    private Integer totalCount;

    private Integer testScore;

    private Integer aiScore;

    private Integer finalScore;

    private String aiReviewMd;

    /**
     * 逐用例判定详情
     */
    private List<TestCaseResult> testCaseResults;

    public static CodingRunResultVO error(String language, String message) {
        return CodingRunResultVO.builder()
                .language(language)
                .status("error")
                .stderr(message)
                .build();
    }

    @Data
    public static class TestCaseResult {
        private Integer testCaseId;
        private Boolean passed;
        private String status;
        private String statusDescription;
        private String actualOutput;
        private String expectedOutput;
        private String stderr;
        private String compileOutput;
        private Boolean isSample;
    }
}
