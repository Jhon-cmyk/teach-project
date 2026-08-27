package com.ruyi.teach.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HomeworkSubmissionReviewRequest {
    @NotNull(message = "提交记录 ID 不能为空")
    @Positive(message = "提交记录 ID 必须为正数")
    private Long submissionId;

    @Size(max = 4000, message = "教师评语长度不能超过 4000 位")
    private String teacherRemark;

    @Valid
    @Size(max = 500, message = "评分明细不能超过 500 项")
    private List<QuestionScore> details;

    @Data
    public static class QuestionScore {
        @NotNull(message = "答题明细 ID 不能为空")
        @Positive(message = "答题明细 ID 必须为正数")
        private Long id;

        @NotNull(message = "题目得分不能为空")
        @PositiveOrZero(message = "题目得分不能为负数")
        private Integer score;
    }
}
