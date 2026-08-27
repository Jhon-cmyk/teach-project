package com.ruyi.teach.model.vo;

import lombok.Data;

@Data
public class PersonalPracticeCreateVO {
    private Long assignmentId;
    private String title;
    private String sourceType;
    private String sourceLabel;
    private Integer questionCount;
}
