package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ScheduleAnalysisRecordVO implements Serializable {

    private Long id;

    private Long userId;

    private String semesterLabel;

    private String sourceFileName;

    private List<String> extractedCourses;

    private List<String> matchedCourses;

    private Map<String, Object> insights;

    private String status;

    private String updateTime;

    private static final long serialVersionUID = 1L;
}