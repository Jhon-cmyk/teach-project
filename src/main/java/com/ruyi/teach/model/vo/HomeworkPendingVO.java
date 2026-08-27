package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 学生待完成作业列表项
 */
@Data
public class HomeworkPendingVO {
    private Long assignmentId;
    private String title;
    private String teacherNote;
    private Date deadline;
    private String courseName;
    private String teacherName;
    private Integer questionCount;
    /** 该学生已提交次数 */
    private Integer attemptCount;
    private Integer maxAttemptCount;
    private Integer allowRedo;

    private Integer durationMinutes;
}