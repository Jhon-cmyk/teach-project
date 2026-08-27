package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("homework_submission")
@Data
public class HomeworkSubmission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long teacherId;

    private Long studentId;

    private Long classId;

    private Long courseId;

    private Integer attemptNo;

    /**
     * draft / submitted / judging / completed / failed
     */
    private String submitStatus;

    private String submissionType;

    private String gradingModeSnapshot;

    private String reviewStatus;

    private Integer aiSuggestedTotalScore;

    private String visionStatus;

    private String visionResultJson;

    private String studentAnswerJson;

    private Integer objectiveScore;

    private Integer subjectiveScore;

    private Integer totalScore;

    private Integer correctCount;

    private Integer wrongCount;

    private String aiReportMarkdown;

    private String aiReportJson;

    private String aiRawResponse;

    /**
     * 教师批阅总评语（考试用）
     */
    private String teacherRemark;

    private Date submitTime;

    private Date judgeTime;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
