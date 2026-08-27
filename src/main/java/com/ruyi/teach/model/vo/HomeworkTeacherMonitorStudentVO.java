package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class HomeworkTeacherMonitorStudentVO implements Serializable {

    private Long studentId;

    private String studentName;

    /**
     * pending / submitted / judging / completed / failed
     */
    private String submitStatus;

    private String reviewStatus;

    private Date submitTime;

    private Integer totalScore;

    private Integer aiSuggestedTotalScore;

    private Integer pendingReviewQuestionCount;

    private Integer correctCount;

    private Integer wrongCount;

    private Long submissionId;

    private static final long serialVersionUID = 1L;
}
