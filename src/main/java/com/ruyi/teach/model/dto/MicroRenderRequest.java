package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MicroRenderRequest {

    @Size(max = 200, message = "微课标题长度不能超过 200 位")
    private String title;

    @NotBlank(message = "微课脚本不能为空")
    @Size(max = 2_000_000, message = "微课脚本过长")
    private String scriptJson;

    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    @Positive(message = "微课时长必须为正数")
    @Max(value = 120, message = "微课时长不能超过 120 分钟")
    private Integer durationMinutes;

    private Boolean subtitlesEnabled;

    @Size(max = 32, message = "微课风格长度不能超过 32 位")
    private String style;

    @Size(max = 32, message = "画质模式长度不能超过 32 位")
    private String qualityMode;

    @Size(max = 128, message = "音色 ID 长度不能超过 128 位")
    private String voiceId;

    private Boolean burnSubtitles;

    private Boolean useAiKeyframes;
}
