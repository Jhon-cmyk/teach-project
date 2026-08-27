package com.ruyi.teach.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HomeworkSubmitRequest {
    @NotNull(message = "作业 ID 不能为空")
    @Positive(message = "作业 ID 必须为正数")
    private Long assignmentId;

    @Pattern(regexp = "^(online|image|mixed)?$", message = "提交类型不合法")
    private String submissionType;

    @Size(max = 1_000_000, message = "作答内容过长")
    private String studentAnswerJson;

    @Size(max = 20, message = "整卷图片不能超过 20 张")
    private List<@Size(max = 2048, message = "图片地址长度不能超过 2048 位") String> wholePaperImageUrls;

    @Valid
    @Size(max = 200, message = "题目图片分组不能超过 200 组")
    private List<QuestionImageItem> questionImageItems;

    @Data
    public static class QuestionImageItem {
        @Size(max = 50, message = "题号长度不能超过 50 位")
        private String questionNo;

        @Size(max = 10, message = "每道题图片不能超过 10 张")
        private List<@Size(max = 2048, message = "图片地址长度不能超过 2048 位") String> imageUrls;
    }
}
