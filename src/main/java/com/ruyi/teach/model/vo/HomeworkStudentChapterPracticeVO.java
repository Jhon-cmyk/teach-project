package com.ruyi.teach.model.vo;

import lombok.Data;

@Data
public class HomeworkStudentChapterPracticeVO {

    private Long assignmentId;
    private String title;

    private Long courseId;
    private Long chapterId;
    private String chapterTitle;

    private Integer questionCount;
    private Integer totalScore;

    /**
     * not_started / judging / completed / failed
     */
    private String submitStatus;

    private Boolean completed;

    private Integer attemptCount;

    private Long latestSubmissionId;

    private Integer latestScore;

    private String teacherNote;
}