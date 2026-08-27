package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MicroScriptRequest {

    @NotBlank(message = "微课主题不能为空")
    @Size(max = 200, message = "微课主题长度不能超过 200 位")
    private String topic;

    @Size(max = 10_000, message = "知识点内容过长")
    private String knowledgePoints;

    @Size(max = 50, message = "年级长度不能超过 50 位")
    private String grade;

    @Positive(message = "微课时长必须为正数")
    @Max(value = 120, message = "微课时长不能超过 120 分钟")
    private Integer durationMinutes;

    @Size(max = 32, message = "微课风格长度不能超过 32 位")
    private String style;

    private Boolean subtitlesEnabled;

    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    private Boolean publishAfterRender;
}
