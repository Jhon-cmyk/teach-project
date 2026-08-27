package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("homework_submission_detail")
@Data
public class HomeworkSubmissionDetail implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private String questionNo;

    private String questionType;

    private String stemSnapshot;

    private String standardAnswer;

    private String studentAnswer;

    private String imageUrlsJson;

    private String recognizedText;

    private Double visionConfidence;

    private Integer fullScore;

    private Integer aiSuggestedScore;

    private Integer score;

    private Integer isCorrect;

    private String aiComment;

    private Date createTime;

    @TableField(exist = false)
    private String optionsJson;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
