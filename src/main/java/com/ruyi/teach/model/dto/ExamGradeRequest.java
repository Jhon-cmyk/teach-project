package com.ruyi.teach.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamGradeRequest {
    private Long submissionId;
    private String teacherRemark;
    private List<QuestionScore> details;

    @Data
    public static class QuestionScore {
        private Long id;
        private Integer score;
    }
}
