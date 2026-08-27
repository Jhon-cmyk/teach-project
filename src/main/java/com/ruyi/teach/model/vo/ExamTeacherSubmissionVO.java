package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ExamTeacherSubmissionVO {
    private Long submissionId;
    private Long studentId;
    private String studentName;
    private String submitStatus;
    private Date submitTime;
    private Integer totalScore;
    private String teacherRemark;
    private String studentAnswerJson;
    private String contentSnapshot;
    private List<ExamQuestionDetailVO> details;

    @Data
    public static class ExamQuestionDetailVO {
        private Long id;
        private String questionNo;
        private String questionType;
        private String stemSnapshot;
        private String standardAnswer;
        private String studentAnswer;
        private String imageUrlsJson;
        private Integer fullScore;
        private Integer score;
        private String aiComment;
    }
}
