package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

@Data
public class HomeworkPublishRequest {
    @NotNull(message = "试卷资源 ID 不能为空")
    @Positive(message = "试卷资源 ID 必须为正数")
    private Long quizResourceId;

    @NotNull(message = "目标班级不能为空")
    @Positive(message = "班级 ID 必须为正数")
    private Long classId;

    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    /**
     * 新增：章节ID
     */
    @Positive(message = "章节 ID 必须为正数")
    private Long chapterId;

    @Pattern(regexp = "^(homework|exam)?$", message = "作业类型不合法")
    private String assignmentType;

    @Size(max = 200, message = "作业标题长度不能超过 200 位")
    private String title;

    @Size(max = 2000, message = "教师说明长度不能超过 2000 位")
    private String teacherNote;

    @Pattern(regexp = "^(online|image|mixed)?$", message = "作答方式不合法")
    private String answerMode;

    @Pattern(regexp = "^(per_question)?$", message = "图片粒度不合法")
    private String imageGranularity;

    @Pattern(regexp = "^(auto|ai_review)?$", message = "批改方式不合法")
    private String gradingMode;
    private Date deadline;

    @Positive(message = "考试时长必须为正数")
    @Max(value = 1440, message = "考试时长不能超过 1440 分钟")
    private Integer durationMinutes;

    @jakarta.validation.constraints.Min(value = 0, message = "是否允许重做只能为 0 或 1")
    @Max(value = 1, message = "是否允许重做只能为 0 或 1")
    private Integer allowRedo;

    @Positive(message = "最大作答次数必须为正数")
    @Max(value = 100, message = "最大作答次数不能超过 100")
    private Integer maxAttemptCount;
}
