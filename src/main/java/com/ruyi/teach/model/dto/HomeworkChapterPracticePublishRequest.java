package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HomeworkChapterPracticePublishRequest {

    @NotNull(message = "练习资源 ID 不能为空")
    @Positive(message = "练习资源 ID 必须为正数")
    private Long quizResourceId;

    @NotNull(message = "目标班级不能为空")
    @Positive(message = "班级 ID 必须为正数")
    private Long classId;

    @NotNull(message = "课程 ID 不能为空")
    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    @NotNull(message = "章节 ID 不能为空")
    @Positive(message = "章节 ID 必须为正数")
    private Long chapterId;

    @Size(max = 200, message = "练习标题长度不能超过 200 位")
    private String title;

    @Size(max = 2000, message = "教师说明长度不能超过 2000 位")
    private String teacherNote;
}
