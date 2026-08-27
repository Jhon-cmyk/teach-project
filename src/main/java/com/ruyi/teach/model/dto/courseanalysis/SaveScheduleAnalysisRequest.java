package com.ruyi.teach.model.dto.courseanalysis;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class SaveScheduleAnalysisRequest implements Serializable {

    /**
     * 由后端根据登录态注入
     */
    private Long userId;

    private String semesterLabel;

    private String sourceFileName;

    private String sourceFileUrl;

    private List<String> extractedCourses;

    private List<String> matchedCourses;

    private Map<String, Object> insights;

    private static final long serialVersionUID = 1L;
}