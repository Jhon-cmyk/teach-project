package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class HomeworkStudentChapterCurrentVO {

    private Long assignmentId;
    private String title;

    private Long courseId;
    private Long chapterId;
    private String chapterTitle;

    private Date deadline;

    private Integer questionCount;
    private Integer totalScore;

    /**
     * not_started / submitted / judging / completed / failed
     */
    private String submitStatus;

    /**
     * 当前最近一次是否已完成
     */
    private Boolean completed;

    private Integer attemptCount;
    private Integer maxAttemptCount;

    private Integer allowRedo;

    private Long latestSubmissionId;

    /**
     * 最近一次得分（已完成时前端可直接展示）
     */
    private Integer latestScore;

    private String teacherNote;
}