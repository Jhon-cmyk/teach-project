package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称长度不能超过 100 位")
    private String name;

    @Size(max = 5000, message = "课程简介长度不能超过 5000 位")
    private String description;

    @Size(max = 2048, message = "封面地址长度不能超过 2048 位")
    private String coverImg;

    @Size(max = 32, message = "课程类型长度不能超过 32 位")
    private String type;

    private Boolean faceDetectionRequired;

    @Size(max = 200, message = "单门课程最多关联 200 个班级")
    private List<@Positive(message = "班级 ID 必须为正数") Long> classIds;
}
