package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CodingSubmissionVO {

    private Long id;

    private Long problemId;

    private Long studentId;

    private String studentName;

    private String language;

    private String code;

    private String status;

    private Integer passedCount;

    private Integer totalCount;

    private Integer testScore;

    private Integer aiScore;

    private Integer finalScore;

    private String aiReviewMd;

    private Integer runtimeMs;

    private Integer memoryKb;

    private Date createTime;
}
