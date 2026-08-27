package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 学生历史作答记录
 */
@Data
public class HomeworkHistoryVO {
    private Long submissionId;
    private Long assignmentId;
    private String title;
    private String courseName;
    private Date submitTime;
    private Integer totalScore;
    private Integer correctCount;
    private Integer wrongCount;
    private String submitStatus;
    /** 报告摘要：取 ai_report_markdown 前 100 字 */
    private String reportSummary;
}