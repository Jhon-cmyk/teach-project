package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 学生查看作业详情
 */
@Data
public class HomeworkDetailVO {
    private Long assignmentId;
    private String title;
    private String teacherNote;
    private Date deadline;
    private String contentSnapshot;
    private String paramsSnapshot;
    private String answerMode;
    private String imageGranularity;
    private String gradingMode;
    private String assignmentType;
    private String sourceType;
    private String courseName;
    private String teacherName;
    private Integer questionCount;
    private Integer totalScore;
    private Integer allowRedo;
    private Integer durationMinutes;
    private Integer maxAttemptCount;
    /** 该学生已提交次数 */
    private Integer attemptCount;
    /** 是否已完成(最近一次是否completed) */
    private Boolean completed;
    private Long latestSubmissionId;
}
