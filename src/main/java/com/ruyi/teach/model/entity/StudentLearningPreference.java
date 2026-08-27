package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("student_learning_preference")
public class StudentLearningPreference implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private String dominantType;

    private Integer videoScore;

    private Integer textScore;

    private Integer practiceScore;

    private Integer discussionScore;

    private Integer aiScore;

    private Integer resourceScore;

    private Integer profileCompleted;

    private String personalityType;

    private String universityName;

    private String developmentGoal;

    private Integer aiQuestionCount;

    private String aiProfileSummary;

    private String aiProfileJson;

    private Date lastAiQuestionTime;

    private String assessmentJson;

    private String summary;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
