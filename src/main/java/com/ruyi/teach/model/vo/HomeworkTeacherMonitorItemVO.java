package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class HomeworkTeacherMonitorItemVO implements Serializable {

    private Long assignmentId;

    private String title;

    /**
     * 练习题资源ID（ai_resource.id），用于前端按练习题筛选
     */
    private Long quizResourceId;

    /**
     * 练习题标题（取自 quizTitleSnapshot，为空时回退到 title）
     */
    private String quizTitle;

    private Long classId;

    private Long courseId;

    private Date publishTime;

    private Date deadline;

    private Integer questionCount;

    private Integer totalScore;

    private Integer studentTotal;

    private Integer completedCount;

    private Integer reviewPendingCount;

    private Integer pendingCount;

    /**
     * 0 ~ 100
     */
    private Double completionRate;

    private Double avgScore;

    private Integer lowScoreCount;

    /**
     * published / closed
     */
    private String status;

    /**
     * homework / exam
     */
    private String assignmentType;

    private String answerMode;

    private String imageGranularity;

    private String gradingMode;

    /**
     * 考试时长（分钟），仅 assignmentType='exam' 时有值
     */
    private Integer durationMinutes;

    private static final long serialVersionUID = 1L;
}
