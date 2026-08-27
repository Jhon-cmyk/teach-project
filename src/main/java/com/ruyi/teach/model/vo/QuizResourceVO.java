package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class QuizResourceVO {
    private Long id;
    private String title;
    private String scenario;
    private Integer questionCount;
    private String content;
    private String paramsJson;
    private Integer isPublished;
    private Date createTime;
}
