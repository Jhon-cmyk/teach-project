package com.ruyi.teach.model.dto;

import lombok.Data;

@Data
public class TeacherScheduleUpdateRequest {
    private Long id;
    private String courseName;
    private Long linkedCourseId;
    private String className;
    private Long teachingPlanId;
    private Integer weekStart;
    private Integer weekEnd;
    private Integer dayOfWeek;
    private Integer startPeriod;
    private Integer endPeriod;
    private String semesterLabel;
}
