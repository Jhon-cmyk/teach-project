package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FatigueReportRequest {

    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    @Positive(message = "章节 ID 必须为正数")
    private Long chapterId;

    @PositiveOrZero(message = "打哈欠次数不能为负数")
    private Integer yawnCount;

    @PositiveOrZero(message = "疲劳次数不能为负数")
    private Integer fatigueCount;

    @PositiveOrZero(message = "未检测到人脸次数不能为负数")
    private Integer noFaceCount;

    @PositiveOrZero(message = "正常检测次数不能为负数")
    private Integer normalCount;

    @PositiveOrZero(message = "检测总次数不能为负数")
    private Integer totalDetections;

    @PositiveOrZero(message = "监测时长不能为负数")
    @Max(value = 86400, message = "单日监测时长不能超过 86400 秒")
    private Integer monitorSeconds;

    @Size(max = 1_000_000, message = "疲劳事件数据过长")
    private String events;

    @Size(max = 1_000_000, message = "EAR 采样数据过长")
    private String earSamples;

    @Size(max = 1_000_000, message = "MAR 采样数据过长")
    private String marSamples;

    @Pattern(
            regexp = "^(normal|fatigue|yawn|no_face)?$",
            message = "疲劳状态值不合法"
    )
    private String lastStatus;
}
