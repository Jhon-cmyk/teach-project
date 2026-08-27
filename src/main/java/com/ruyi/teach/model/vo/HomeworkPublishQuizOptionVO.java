package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class HomeworkPublishQuizOptionVO {
    private Long quizResourceId;
    private String title;
    private Date createTime;
}